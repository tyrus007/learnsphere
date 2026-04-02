package com.ashique.courseservice.services;

import com.ashique.courseservice.dto.CreateLessonRequest;
import com.ashique.courseservice.dto.InternalLessonExistsResponse;
import com.ashique.courseservice.dto.LessonResponse;
import com.ashique.courseservice.entity.Lesson;
import com.ashique.courseservice.entity.Module;
import com.ashique.courseservice.repository.LessonRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LessonService {

    private final LessonRepository lessonRepository;
    private final ModuleService moduleService;
    private final CourseService courseService;

    @Transactional
    public LessonResponse createLesson(UUID userId, UUID moduleId, CreateLessonRequest request) {
        Module module = moduleService.getModuleByIdForUpdate(moduleId);
        courseService.validateOwnership(userId, module.getCourse());
        int nextPosition = lessonRepository.findTopByModuleIdOrderByPositionDesc(moduleId)
                .map(Lesson::getPosition)
                .orElse(0) + 1;

        Lesson lesson = Lesson.builder()
                .module(module)
                .title(request.getTitle())
                .contentType(request.getContentType())
                .contentUrlOrBody(request.getContentUrlOrBody())
                .position(nextPosition)
                .isPreview(Boolean.TRUE.equals(request.getIsPreview()))   // using .equals() makes it nulls safe. if isPreview is null it will become false
                .build();

        return toLessonResponse(lessonRepository.save(lesson));
    }

    public Lesson getLessonById(UUID lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new com.ashique.courseservice.exceptions.ResourceNotFoundException(
                        "Lesson not found with id: " + lessonId));
    }

    public InternalLessonExistsResponse getLessonExists(UUID lessonId) {
        return lessonRepository.findById(lessonId)
                .map(lesson -> InternalLessonExistsResponse.builder()
                        .exists(true)
                        .courseId(lesson.getModule().getCourse().getId())
                        .build())
                .orElseGet(() -> InternalLessonExistsResponse.builder()
                        .exists(false)
                        .build());
    }

    private LessonResponse toLessonResponse(Lesson lesson) {
        return LessonResponse.builder()
                .id(lesson.getId())
                .moduleId(lesson.getModule().getId())
                .title(lesson.getTitle())
                .contentType(lesson.getContentType())
                .contentUrlOrBody(lesson.getContentUrlOrBody())
                .position(lesson.getPosition())
                .isPreview(lesson.getIsPreview())
                .build();
    }
}
