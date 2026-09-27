package com.authshield360.school;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByClassroomIdOrderByDueAtDesc(Long classroomId);

    List<Assignment> findByCreatedByOrderByCreatedAtDesc(Long createdBy);

    List<Assignment> findByClassroomIdInOrderByDueAtDesc(Collection<Long> classroomIds);
}
