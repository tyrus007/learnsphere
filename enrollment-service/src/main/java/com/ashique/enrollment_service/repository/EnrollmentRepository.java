package com.ashique.enrollment_service.repository;

import com.ashique.enrollment_service.entity.Enrollment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, UUID> {

    boolean existsByStudentIdAndCourseId(UUID studentId, UUID courseId);

    List<Enrollment> findAllByStudentIdOrderByEnrolledAtDesc(UUID studentId);

    List<Enrollment> findAllByCourseIdOrderByEnrolledAtDesc(UUID courseId);

    Optional<Enrollment> findByStudentIdAndCourseId(UUID studentId, UUID courseId);
}
