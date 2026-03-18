package com.ashique.courseservice.repository;

import com.ashique.courseservice.entity.Lesson;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LessonRepository extends JpaRepository<Lesson, UUID> {
}
