package com.authshield360.school;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "enrollments", uniqueConstraints =
        @UniqueConstraint(name = "uq_enrollment", columnNames = {"classroom_id", "student_id"}))
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Enrollment() { }

    public Enrollment(Long classroomId, Long studentId) {
        this.classroomId = classroomId;
        this.studentId = studentId;
    }

    public Long getId() { return id; }
    public Long getClassroomId() { return classroomId; }
    public Long getStudentId() { return studentId; }
    public Instant getCreatedAt() { return createdAt; }
}
