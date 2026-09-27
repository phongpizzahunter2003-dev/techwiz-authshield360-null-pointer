package com.authshield360.user;

import com.authshield360.audit.AuditAction;
import com.authshield360.audit.AuditEvent;
import com.authshield360.audit.AuditService;
import com.authshield360.common.BusinessException;
import com.authshield360.common.ErrorCode;
import com.authshield360.common.PageResult;
import com.authshield360.school.StudentProfile;
import com.authshield360.school.StudentProfileRepository;
import com.authshield360.school.TeacherProfile;
import com.authshield360.school.TeacherProfileRepository;
import com.authshield360.security.SecurityUtils;
import com.authshield360.user.dto.CreateUserRequest;
import com.authshield360.user.dto.UpdateUserRequest;
import com.authshield360.user.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Account & role management (UC-07). */
@Service
public class UserService {

    private final UserRepository users;
    private final StudentProfileRepository studentProfiles;
    private final TeacherProfileRepository teacherProfiles;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public UserService(UserRepository users, StudentProfileRepository studentProfiles,
                       TeacherProfileRepository teacherProfiles, PasswordEncoder passwordEncoder,
                       AuditService audit) {
        this.users = users;
        this.studentProfiles = studentProfiles;
        this.teacherProfiles = teacherProfiles;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional(readOnly = true)
    public User findOrThrow(Long id) {
        return users.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public User findByUsernameOrThrow(String username) {
        return users.findByUsername(username).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public PageResult<UserResponse> search(RoleType role, String q, Pageable pageable) {
        Page<User> page = users.search(role, (q == null || q.isBlank()) ? null : q.trim(), pageable);
        return PageResult.of(page, UserService::toResponse);
    }

    @Transactional
    public UserResponse create(CreateUserRequest req) {
        if (users.existsByUsername(req.username())) {
            throw new BusinessException(ErrorCode.USERNAME_EXISTS);
        }
        if (users.existsByEmail(req.email())) {
            throw new BusinessException(ErrorCode.EMAIL_EXISTS);
        }
        User user = new User();
        user.setUsername(req.username().trim());
        user.setEmail(req.email().trim().toLowerCase());
        user.setPhone(req.phone());
        user.setFullName(req.fullName());
        user.setRole(req.role());
        user.setStatus(UserStatus.ACTIVE);
        user.setMfaEnabled(req.role() != RoleType.STUDENT);
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        User saved = users.save(user);
        createProfile(saved);
        audit.record(AuditEvent.action(AuditAction.USER_CREATE).success()
                .user(actor()).detail("targetUser", saved.getUsername()).detail("role", saved.getRole().name()));
        return toResponse(saved);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest req) {
        User user = findOrThrow(id);
        boolean roleChanged = false;
        if (req.email() != null && !req.email().equalsIgnoreCase(user.getEmail())) {
            if (users.existsByEmail(req.email())) throw new BusinessException(ErrorCode.EMAIL_EXISTS);
            user.setEmail(req.email().trim().toLowerCase());
        }
        if (req.phone() != null) user.setPhone(req.phone());
        if (req.fullName() != null) user.setFullName(req.fullName());
        if (req.role() != null && req.role() != user.getRole()) {
            user.setRole(req.role());
            roleChanged = true;
            createProfile(user);
        }
        if (req.status() != null) {
            user.setStatus(req.status());
            if (req.status() == UserStatus.ACTIVE) {
                user.setFailedAttempts(0);
                user.setLockoutLevel(0);
                user.setLockedUntil(null);
            }
        }
        if (req.password() != null && !req.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        User saved = users.save(user);
        String action = roleChanged ? AuditAction.ROLE_ASSIGN : AuditAction.USER_UPDATE;
        audit.record(AuditEvent.action(action).success()
                .user(actor()).detail("targetUser", saved.getUsername()).detail("role", saved.getRole().name()));
        return toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        User user = findOrThrow(id);
        users.delete(user);
        audit.record(AuditEvent.action(AuditAction.USER_DELETE).success()
                .user(actor()).detail("targetUser", user.getUsername()));
    }

    @Transactional
    public void resetMfa(Long id) {
        User user = findOrThrow(id);
        user.setMfaEnrolled(false);
        user.setMfaSecretEnc(null);
        users.save(user);
        audit.record(AuditEvent.action(AuditAction.MFA_RESET).success()
                .user(actor()).detail("targetUser", user.getUsername()));
    }

    private void createProfile(User user) {
        switch (user.getRole()) {
            case STUDENT -> studentProfiles.findByUserId(user.getId()).orElseGet(() -> {
                StudentProfile p = new StudentProfile();
                p.setUserId(user.getId());
                p.setStudentCode(String.format("SV%05d", user.getId()));
                p.setClassName("Chưa xếp lớp");
                return studentProfiles.save(p);
            });
            case TEACHER -> teacherProfiles.findByUserId(user.getId()).orElseGet(() -> {
                TeacherProfile p = new TeacherProfile();
                p.setUserId(user.getId());
                p.setEmployeeCode(String.format("GV%05d", user.getId()));
                p.setDepartment("Tổ Tin học");
                return teacherProfiles.save(p);
            });
            default -> { /* ADMIN has no profile row */ }
        }
    }

    private String actor() {
        return SecurityUtils.currentOrEmpty().map(cu -> cu.username()).orElse("system");
    }

    public static UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getPhone(), u.getFullName(),
                u.getRole(), u.getStatus(), u.isMfaEnabled(), u.isMfaEnrolled(), u.getFailedAttempts(),
                u.getLockoutLevel(), u.getLockedUntil(), u.getCreatedAt());
    }
}
