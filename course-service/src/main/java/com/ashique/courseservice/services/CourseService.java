package com.ashique.courseservice.services;

import com.ashique.courseservice.dto.CourseDetailResponse;
import com.ashique.courseservice.dto.CourseResponse;
import com.ashique.courseservice.dto.CreateCourseRequest;
import com.ashique.courseservice.dto.LessonResponse;
import com.ashique.courseservice.dto.ModuleResponse;
import com.ashique.courseservice.dto.UpdateCourseRequest;
import com.ashique.courseservice.entity.Course;
import com.ashique.courseservice.entity.CourseStatus;
import com.ashique.courseservice.entity.Lesson;
import com.ashique.courseservice.entity.Module;
import com.ashique.courseservice.exceptions.ForbiddenOperationException;
import com.ashique.courseservice.exceptions.ResourceNotFoundException;
import com.ashique.courseservice.repository.CourseRepository;
import com.ashique.courseservice.repository.LessonRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final LessonRepository lessonRepository;

    @Transactional
    public CourseResponse createCourse(UUID userId, CreateCourseRequest request) {
        Course course = Course.builder()
                .instructorId(userId)
                .title(request.getTitle())
                .description(request.getDescription())
                .level(request.getLevel())
                .category(request.getCategory())
                .status(CourseStatus.DRAFT)
                .build();

        return toCourseResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse updateCourse(UUID userId, UUID courseId, UpdateCourseRequest request) {
        Course course = getOwnedCourse(userId, courseId);
        course.setTitle(request.getTitle());
        course.setDescription(request.getDescription());
        course.setLevel(request.getLevel());
        course.setCategory(request.getCategory());

        return toCourseResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse publishCourse(UUID userId, UUID courseId) {
        Course course = getOwnedCourse(userId, courseId);
        course.setStatus(CourseStatus.PUBLISHED);
        return toCourseResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse unpublishCourse(UUID userId, UUID courseId) {
        Course course = getOwnedCourse(userId, courseId);
        course.setStatus(CourseStatus.DRAFT);
        return toCourseResponse(courseRepository.save(course));
    }

    public List<CourseResponse> getPublishedCourses() {
        return courseRepository.findAllByStatusOrderByCreatedAtDesc(CourseStatus.PUBLISHED)
                .stream()
                .map(this::toCourseResponse)
                .toList();
    }

    public CourseDetailResponse getCourseDetail(UUID courseId) {
        Course course = getCourseById(courseId);

        return CourseDetailResponse.builder()
                .id(course.getId())
                .instructorId(course.getInstructorId())
                .title(course.getTitle())
                .description(course.getDescription())
                .level(course.getLevel())
                .category(course.getCategory())
                .status(course.getStatus())
                .createdAt(course.getCreatedAt())
                .modules(course.getModules().stream().map(this::toModuleResponse).toList())
                .build();
    }

    public Course getCourseById(UUID courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
    }

    public long getLessonCountByCourseId(UUID courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }

        return lessonRepository.countByModuleCourseId(courseId);
    }

    public Course getOwnedCourse(UUID userId, UUID courseId) {
        Course course = getCourseById(courseId);
        validateOwnership(userId, course);
        return course;
    }

    public Course getOwnedCourseForUpdate(UUID userId, UUID courseId) {
        Course course = courseRepository.findByIdForUpdate(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));
        validateOwnership(userId, course);
        return course;
    }

    public void validateOwnership(UUID userId, Course course) {
        if (!course.getInstructorId().equals(userId)) {
            throw new ForbiddenOperationException("You do not have access to this course");
        }
    }

    private CourseResponse toCourseResponse(Course course) {
        return CourseResponse.builder()
                .id(course.getId())
                .instructorId(course.getInstructorId())
                .title(course.getTitle())
                .description(course.getDescription())
                .level(course.getLevel())
                .category(course.getCategory())
                .status(course.getStatus())
                .createdAt(course.getCreatedAt())
                .build();
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
