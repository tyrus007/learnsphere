package com.ashique.courseservice.controllers;

import com.ashique.courseservice.dto.CourseDetailResponse;
import com.ashique.courseservice.dto.CourseResponse;
import com.ashique.courseservice.dto.CreateCourseRequest;
import com.ashique.courseservice.dto.UpdateCourseRequest;
import com.ashique.courseservice.entity.Course;
import com.ashique.courseservice.entity.CourseStatus;
import com.ashique.courseservice.exceptions.ForbiddenOperationException;
import com.ashique.courseservice.services.CourseService;
import com.ashique.learnsphere.jwt.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseService.createCourse(getAuthenticatedUser().userId(), request));
    }

    @PutMapping("/{courseId}")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<CourseResponse> updateCourse(
            @PathVariable UUID courseId,
            @Valid @RequestBody UpdateCourseRequest request) {
        return ResponseEntity.ok(courseService.updateCourse(getAuthenticatedUser().userId(), courseId, request));
    }

    @PatchMapping("/{courseId}/publish")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<CourseResponse> publishCourse(@PathVariable UUID courseId) {
        return ResponseEntity.ok(courseService.publishCourse(getAuthenticatedUser().userId(), courseId));
    }

    @PatchMapping("/{courseId}/unpublish")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<CourseResponse> unpublishCourse(@PathVariable UUID courseId) {
        return ResponseEntity.ok(courseService.unpublishCourse(getAuthenticatedUser().userId(), courseId));
    }

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getPublishedCourses() {
        return ResponseEntity.ok(courseService.getPublishedCourses());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<List<CourseResponse>> getMyCourses() {
        return ResponseEntity.ok(courseService.getMyCourses(getAuthenticatedUser().userId()));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDetailResponse> getCourseDetail(@PathVariable UUID courseId) {
        Course course = courseService.getCourseById(courseId);

        if (course.getStatus() != CourseStatus.PUBLISHED) {       // Not_Published courses are only accessed by Instructor
            AuthenticatedUser authenticatedUser = getAuthenticatedUserOrNull();
            if (authenticatedUser == null || !course.getInstructorId().equals(authenticatedUser.userId())) {
                throw new ForbiddenOperationException("You do not have access to this course");
            }
        }

        return ResponseEntity.ok(courseService.getCourseDetail(courseId));
    }

    private AuthenticatedUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }

        throw new ForbiddenOperationException("Authentication is required");
    }

    private AuthenticatedUser getAuthenticatedUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }

        return null;
    }
}
