package com.ashique.courseservice.services;

import com.ashique.courseservice.dto.CreateModuleRequest;
import com.ashique.courseservice.dto.LessonResponse;
import com.ashique.courseservice.dto.ModuleResponse;
import com.ashique.courseservice.entity.Course;
import com.ashique.courseservice.entity.Lesson;
import com.ashique.courseservice.entity.Module;
import com.ashique.courseservice.exceptions.ResourceNotFoundException;
import com.ashique.courseservice.repository.ModuleRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModuleService {

    private final ModuleRepository moduleRepository;
    private final CourseService courseService;

    @Transactional
    public ModuleResponse createModule(UUID userId, UUID courseId, CreateModuleRequest request) {
        Course course = courseService.getOwnedCourseForUpdate(userId, courseId);
        int nextPosition = moduleRepository.findTopByCourseIdOrderByPositionDesc(courseId)
                .map(Module::getPosition)
                .orElse(0) + 1;

        Module module = Module.builder()
                .course(course)
                .title(request.getTitle())
                .position(nextPosition)
                .build();

        return toModuleResponse(moduleRepository.save(module));
    }

    public List<ModuleResponse> getModulesByCourseId(UUID courseId) {
        courseService.getCourseById(courseId);

        return moduleRepository.findAllByCourseIdOrderByPositionAsc(courseId)
                .stream()
                .map(this::toModuleResponse)
                .toList();
    }

    public Module getModuleById(UUID moduleId) {
        return moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + moduleId));
    }

    public Module getModuleByIdForUpdate(UUID moduleId) {
        return moduleRepository.findByIdForUpdate(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + moduleId));
    }

    private ModuleResponse toModuleResponse(Module module) {
        return ModuleResponse.builder()
                .id(module.getId())
                .courseId(module.getCourse().getId())
                .title(module.getTitle())
                .position(module.getPosition())
                .lessons(module.getLessons().stream().map(this::toLessonResponse).toList())
                .build();
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
