package com.authshield360.school;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExamResultRepository extends JpaRepository<ExamResult, Long> {
    List<ExamResult> findByStudentIdOrderByExamDateDesc(Long studentId);
    List<ExamResult> findByStudentIdInOrderByExamDateDesc(List<Long> studentIds);
}
