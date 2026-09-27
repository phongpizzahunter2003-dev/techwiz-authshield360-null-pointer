package com.authshield360.school;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ClassroomRepository extends JpaRepository<Classroom, Long> {
    List<Classroom> findAllByOrderByNameAsc();
    List<Classroom> findByTeacherIdOrderByNameAsc(Long teacherId);
    boolean existsByCode(String code);
}
