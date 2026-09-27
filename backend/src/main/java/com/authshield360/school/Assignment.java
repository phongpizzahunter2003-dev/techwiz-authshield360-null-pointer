package com.authshield360.school;

import jakarta.persistence.*;
import java.time.Instant;

/** Homework/assignment definition. Policy fields drive UC-A1..UC-A4. */
@Entity
@Table(name = "assignments", indexes = {
        @Index(name = "ix_assignments_classroom", columnList = "classroom_id"),
        @Index(name = "ix_assignments_status_due", columnList = "status,due_at")
})
public class Assignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(length = 4000)
    private String description;

    @Column(name = "classroom_id", nullable = false)
    private Long classroomId;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "allow_late", nullable = false)
    private boolean allowLate = true;

    @Column(name = "late_cutoff_at")
    private Instant lateCutoffAt;

    @Column(name = "late_penalty_pct", nullable = false)
    private int latePenaltyPct = 10;

    @Column(name = "allow_resubmission", nullable = false)
    private boolean allowResubmission = true;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts = 3;

    @Column(name = "max_score", nullable = false)
    private int maxScore = 100;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private AssignmentStatus status = AssignmentStatus.PUBLISHED;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getClassroomId() { return classroomId; }
    public void setClassroomId(Long classroomId) { this.classroomId = classroomId; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }
    public boolean isAllowLate() { return allowLate; }
    public void setAllowLate(boolean allowLate) { this.allowLate = allowLate; }
    public Instant getLateCutoffAt() { return lateCutoffAt; }
    public void setLateCutoffAt(Instant lateCutoffAt) { this.lateCutoffAt = lateCutoffAt; }
    public int getLatePenaltyPct() { return latePenaltyPct; }
    public void setLatePenaltyPct(int latePenaltyPct) { this.latePenaltyPct = latePenaltyPct; }
    public boolean isAllowResubmission() { return allowResubmission; }
    public void setAllowResubmission(boolean allowResubmission) { this.allowResubmission = allowResubmission; }
    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public int getMaxScore() { return maxScore; }
    public void setMaxScore(int maxScore) { this.maxScore = maxScore; }
    public AssignmentStatus getStatus() { return status; }
    public void setStatus(AssignmentStatus status) { this.status = status; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
