package com.authshield360.school;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByStudentId(Long studentId);
    List<Enrollment> findByClassroomId(Long classroomId);
    boolean existsByClassroomIdAndStudentId(Long classroomId, Long studentId);
    long countByClassroomId(Long classroomId);
    void deleteByClassroomIdAndStudentId(Long classroomId, Long studentId);
}
