package com.ashique.courseservice.repository;

import com.ashique.courseservice.entity.Lesson;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
    List<Lesson> findAllByModuleIdOrderByPositionAsc(UUID moduleId);

    Optional<Lesson> findTopByModuleIdOrderByPositionDesc(UUID moduleId);

    long countByModuleCourseId(UUID courseId);
}
