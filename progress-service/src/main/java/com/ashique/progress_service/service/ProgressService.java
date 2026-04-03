package com.ashique.progress_service.service;

import com.ashique.progress_service.client.CourseServiceClient;
import com.ashique.progress_service.client.EnrollmentServiceClient;
import com.ashique.progress_service.client.client_dto.InternalCourseLessonCountResponse;
import com.ashique.progress_service.client.client_dto.InternalLessonExistsResponse;
import com.ashique.progress_service.dto.CourseProgressResponse;
import com.ashique.progress_service.dto.LessonProgressResponse;
import com.ashique.progress_service.entity.LessonProgress;
import com.ashique.progress_service.exception.ForbiddenOperationException;
import com.ashique.progress_service.exception.InvalidRequestException;
import com.ashique.progress_service.repository.LessonProgressRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProgressService {

    private final LessonProgressRepository lessonProgressRepository;
    private final CourseServiceClient courseServiceClient;
    private final EnrollmentServiceClient enrollmentServiceClient;

    /**
     * Marks a lesson as complete for the authenticated student.
     * Idempotent — if the lesson is already completed, returns the existing record.
     */
    @Transactional
    public LessonProgressResponse markLessonComplete(
            UUID userId,
            UUID lessonId,
            UUID courseId,
            String authorizationHeader) {

        // 1. Verify enrollment
        boolean enrolled = enrollmentServiceClient.isEnrolled(userId, courseId, authorizationHeader);
        if (!enrolled) {
            throw new ForbiddenOperationException("Student is not enrolled in this course");
        }

        // 2. Verify lesson exists and belongs to this course
        InternalLessonExistsResponse lessonExists =
                courseServiceClient.getLessonExists(lessonId, authorizationHeader);
        if (!Boolean.TRUE.equals(lessonExists.exists())) {
            throw new InvalidRequestException("Lesson not found with id: " + lessonId);
        }
        if (!courseId.equals(lessonExists.courseId())) {
            throw new InvalidRequestException("Lesson does not belong to the specified course");
        }

        // 3. Idempotent: if already completed, return existing record
        Optional<LessonProgress> existing =
                lessonProgressRepository.findByStudentIdAndLessonId(userId, lessonId);
        if (existing.isPresent()) {
            return toLessonResponse(existing.get());
        }

        // 4. Save new progress record
        LessonProgress progress = lessonProgressRepository.save(LessonProgress.builder()
                .studentId(userId)
                .courseId(courseId)
                .lessonId(lessonId)
                .build());

        return toLessonResponse(progress);
    }

    /**
     * Returns the progress for a specific course: completed lessons, total lessons, percentage.
     */
    public CourseProgressResponse getCourseProgress(
            UUID userId,
            UUID courseId,
            String authorizationHeader) {

        long completedLessons = lessonProgressRepository.countByStudentIdAndCourseId(userId, courseId);

        InternalCourseLessonCountResponse lessonCountResponse =
                courseServiceClient.getLessonCount(courseId, authorizationHeader);
        long totalLessons = lessonCountResponse.lessonCount() != null ? lessonCountResponse.lessonCount() : 0;

        double percentage = totalLessons > 0
                ? Math.round((double) completedLessons / totalLessons * 10000.0) / 100.0
                : 0.0;

        return CourseProgressResponse.builder()
                .courseId(courseId)
                .completedLessons(completedLessons)
                .totalLessons(totalLessons)
                .percentage(percentage)
                .build();
    }

    /**
     * Returns a dashboard aggregating progress across all courses the student has tracked.
     * Groups lesson_progress records by courseId and builds a summary for each.
     */
    public List<CourseProgressResponse> getMyDashboard(UUID userId, String authorizationHeader) {
        List<LessonProgress> allProgress = lessonProgressRepository.findAllByStudentId(userId);

        // Group by courseId and count completed lessons per course
        Map<UUID, Long> completedByCourse = allProgress.stream()
                .collect(Collectors.groupingBy(LessonProgress::getCourseId, Collectors.counting()));

        return completedByCourse.entrySet().stream()
                .map(entry -> {
                    UUID courseId = entry.getKey();
                    long completedLessons = entry.getValue();

                    long totalLessons;
                    try {
                        InternalCourseLessonCountResponse countResponse =
                                courseServiceClient.getLessonCount(courseId, authorizationHeader);
                        totalLessons = countResponse.lessonCount() != null ? countResponse.lessonCount() : 0;
                    } catch (Exception ex) {
                        // If Course Service is unavailable for one course, skip percentage
                        totalLessons = 0;
                    }

                    double percentage = totalLessons > 0
                            ? Math.round((double) completedLessons / totalLessons * 10000.0) / 100.0
                            : 0.0;

                    return CourseProgressResponse.builder()
                            .courseId(courseId)
                            .completedLessons(completedLessons)
                            .totalLessons(totalLessons)
                            .percentage(percentage)
                            .build();
                })
                .toList();
    }

    private LessonProgressResponse toLessonResponse(LessonProgress progress) {
        return LessonProgressResponse.builder()
                .lessonId(progress.getLessonId())
                .completedAt(progress.getCompletedAt())
                .build();
    }
}
