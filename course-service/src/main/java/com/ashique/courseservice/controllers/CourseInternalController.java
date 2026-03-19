package com.ashique.courseservice.controllers;

import com.ashique.courseservice.dto.InternalCourseExistsResponse;
import com.ashique.courseservice.dto.InternalCourseLessonCountResponse;
import com.ashique.courseservice.dto.InternalLessonExistsResponse;
import com.ashique.courseservice.services.CourseService;
import com.ashique.courseservice.services.LessonService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal")
public class CourseInternalController {

    private final CourseService courseService;
    private final LessonService lessonService;

    public CourseInternalController(CourseService courseService, LessonService lessonService) {
        this.courseService = courseService;
        this.lessonService = lessonService;
    }

    @GetMapping("/courses/{courseId}/exists")
    public ResponseEntity<InternalCourseExistsResponse> getCourseExists(@PathVariable UUID courseId) {
        return ResponseEntity.ok(courseService.getCourseExists(courseId));
    }

    @GetMapping("/courses/{courseId}/lesson-count")
    public ResponseEntity<InternalCourseLessonCountResponse> getLessonCount(@PathVariable UUID courseId) {
        return ResponseEntity.ok(courseService.getLessonCountResponse(courseId));
    }

    @GetMapping("/lessons/{lessonId}/exists")
    public ResponseEntity<InternalLessonExistsResponse> getLessonExists(@PathVariable UUID lessonId) {
        return ResponseEntity.ok(lessonService.getLessonExists(lessonId));
    }
}
