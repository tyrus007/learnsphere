package com.ashique.progress_service.repository;

import com.ashique.progress_service.entity.LessonProgress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonProgressRepository extends JpaRepository<LessonProgress, UUID> {

    boolean existsByStudentIdAndLessonId(UUID studentId, UUID lessonId);

    Optional<LessonProgress> findByStudentIdAndLessonId(UUID studentId, UUID lessonId);

    long countByStudentIdAndCourseId(UUID studentId, UUID courseId);

    List<LessonProgress> findAllByStudentId(UUID studentId);
}
