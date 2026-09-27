package com.authshield360.notification;

import com.authshield360.common.BusinessException;
import com.authshield360.common.ErrorCode;
import com.authshield360.common.PageResult;
import com.authshield360.notification.dto.NotificationResponse;
import com.authshield360.school.Assignment;
import com.authshield360.school.Classroom;
import com.authshield360.school.ClassroomRepository;
import com.authshield360.school.Enrollment;
import com.authshield360.school.EnrollmentRepository;
import com.authshield360.user.RoleType;
import com.authshield360.user.User;
import com.authshield360.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

/**
 * Creates and manages in-app notifications for students and teachers.
 * Read state transitions: UNREAD -> READ (single or bulk).
 */
@Service
public class NotificationService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneOffset.UTC);

    private final NotificationRepository repository;
    private final EnrollmentRepository enrollments;
    private final ClassroomRepository classrooms;
    private final UserRepository users;

    public NotificationService(NotificationRepository repository, EnrollmentRepository enrollments,
                               ClassroomRepository classrooms, UserRepository users) {
        this.repository = repository;
        this.enrollments = enrollments;
        this.classrooms = classrooms;
        this.users = users;
    }

    // ------------------------------------------------------------- creation

    @Transactional
    public void notifyUser(Long userId, String role, String type, String title, String message, String targetUrl) {
        if (userId == null) {
            return;
        }
        Notification n = new Notification();
        n.setUserId(userId);
        n.setRecipientRole(role);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setTargetUrl(targetUrl);
        n.setCreatedAt(Instant.now());
        repository.save(n);
    }

    @Transactional
    public void notifyUsers(Collection<Long> userIds, String type, String title, String message, String targetUrl) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        userIds.stream().distinct()
                .forEach(id -> notifyUser(id, RoleType.STUDENT.name(), type, title, message, targetUrl));
    }

    /** Every student enrolled in the classroom. */
    @Transactional
    public void notifyClassroom(Long classroomId, String type, String title, String message, String targetUrl) {
        List<Long> ids = enrollments.findByClassroomId(classroomId).stream()
                .map(Enrollment::getStudentId)
                .toList();
        notifyUsers(ids, type, title, message, targetUrl);
    }

    // ---------------------------------------------------- domain shortcuts

    /** Students are told a new assignment is available. */
    @Transactional
    public void assignmentPublished(Assignment assignment) {
        String classroom = classroomName(assignment.getClassroomId());
        notifyClassroom(assignment.getClassroomId(), NotificationType.ASSIGNMENT_PUBLISHED,
                "New assignment",
                "\"" + assignment.getTitle() + "\" was published for " + classroom
                        + ". Due " + DATE.format(assignment.getDueAt()) + ".",
                "/student/assignments/" + assignment.getId());
    }

    @Transactional
    public void assignmentUpdated(Assignment assignment) {
        String classroom = classroomName(assignment.getClassroomId());
        notifyClassroom(assignment.getClassroomId(), NotificationType.ASSIGNMENT_UPDATED,
                "Assignment updated",
                "\"" + assignment.getTitle() + "\" (" + classroom + ") was updated. Due "
                        + DATE.format(assignment.getDueAt()) + ".",
                "/student/assignments/" + assignment.getId());
    }

    @Transactional
    public void assignmentClosed(Assignment assignment) {
        notifyClassroom(assignment.getClassroomId(), NotificationType.ASSIGNMENT_CLOSED,
                "Assignment closed",
                "\"" + assignment.getTitle() + "\" is now closed. Submissions and updates are locked.",
                "/student/assignments/" + assignment.getId());
    }

    /** The teacher who owns the assignment is told about a new submission. */
    @Transactional
    public void submissionReceived(Assignment assignment, String studentName, boolean late, int attempt) {
        notifyUser(assignment.getCreatedBy(), RoleType.TEACHER.name(),
                late ? NotificationType.SUBMISSION_LATE : NotificationType.SUBMISSION_RECEIVED,
                late ? "Late submission" : "New submission",
                studentName + " submitted \"" + assignment.getTitle() + "\""
                        + (attempt > 1 ? " (attempt #" + attempt + ")" : "")
                        + (late ? " after the deadline." : "."),
                "/teacher/assignments/" + assignment.getId());
    }

    /** The student is told the graded result. */
    @Transactional
    public void submissionGraded(Assignment assignment, Long studentId, Integer score, int maxScore) {
        notifyUser(studentId, RoleType.STUDENT.name(), NotificationType.SUBMISSION_GRADED,
                "Assignment graded",
                "\"" + assignment.getTitle() + "\" was graded: " + score + "/" + maxScore + ".",
                "/student/assignments/" + assignment.getId());
    }

    @Transactional
    public void enrolled(Long studentId, Classroom classroom) {
        notifyUser(studentId, RoleType.STUDENT.name(), NotificationType.CLASSROOM_ENROLLED,
                "Added to a class",
                "You were added to " + classroom.getName() + " (" + classroom.getCode() + ").",
                "/student/assignments");
    }

    // ---------------------------------------------------------- read model

    @Transactional(readOnly = true)
    public PageResult<NotificationResponse> list(Long userId, Boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = Boolean.FALSE.equals(unreadOnly)
                ? repository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                : repository.findByUserIdAndReadOrderByCreatedAtDesc(userId, false, pageable);
        return PageResult.of(page, NotificationService::toResponse);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadFalse(userId);
    }

    @Transactional(readOnly = true)
    public long total(Long userId) {
        return repository.countByUserId(userId);
    }

    /** UNREAD -> READ for one notification (ownership enforced). */
    @Transactional
    public NotificationResponse markRead(Long id, Long userId) {
        Notification n = owned(id, userId);
        if (!n.isRead()) {
            n.setRead(true);
            n.setReadAt(Instant.now());
            repository.save(n);
        }
        return toResponse(n);
    }

    @Transactional
    public int markAllRead(Long userId) {
        return repository.markAllReadForUser(userId, Instant.now());
    }

    @Transactional
    public void delete(Long id, Long userId) {
        repository.delete(owned(id, userId));
    }

    private Notification owned(Long id, Long userId) {
        Notification n = repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        if (!n.getUserId().equals(userId)) {
            // Do not reveal that the notification exists for somebody else.
            throw new BusinessException(ErrorCode.NOT_FOUND);
        }
        return n;
    }

    private String classroomName(Long classroomId) {
        return classrooms.findById(classroomId).map(Classroom::getName).orElse("your class");
    }

    public static NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(),
                n.getTargetUrl(), n.isRead(), n.getReadAt(), n.getCreatedAt());
    }

    /** Convenience used by the seeder to greet freshly created accounts. */
    @Transactional
    public void accountReady(User user) {
        notifyUser(user.getId(), user.getRole().name(), NotificationType.ACCOUNT_READY,
                "Welcome to AuthShield 360",
                "Your account is ready. You can sign in with the password provided by your administrator.",
                user.getRole() == RoleType.STUDENT ? "/student" : user.getRole() == RoleType.TEACHER ? "/teacher" : "/admin");
    }
}
