package com.ashique.courseservice.repository;

import com.ashique.courseservice.entity.Course;
import com.ashique.courseservice.entity.CourseStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, UUID> {
    List<Course> findAllByStatusOrderByCreatedAtDesc(CourseStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)          // used for controlling concurrency. locks the row/record while the users update it
    @Query("select c from Course c where c.id = :courseId")
    Optional<Course> findByIdForUpdate(@Param("courseId") UUID courseId);
}
