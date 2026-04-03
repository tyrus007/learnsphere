package com.ashique.progress_service.controller;

import com.ashique.progress_service.dto.CourseProgressResponse;
import com.ashique.progress_service.dto.LessonProgressResponse;
import com.ashique.progress_service.dto.MarkLessonCompleteRequest;
import com.ashique.progress_service.exception.ForbiddenOperationException;
import com.ashique.progress_service.service.ProgressService;
import com.ashique.learnsphere.jwt.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/progress")
public class ProgressController {

    private final ProgressService progressService;

    public ProgressController(ProgressService progressService) {
        this.progressService = progressService;
    }

    /**
     * POST /api/progress/lessons/{lessonId}/complete
     * Marks a lesson as complete for the authenticated STUDENT.
     */
    @PostMapping("/lessons/{lessonId}/complete")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<LessonProgressResponse> markLessonComplete(
            @PathVariable UUID lessonId,
            @Valid @RequestBody MarkLessonCompleteRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {

        AuthenticatedUser user = getAuthenticatedUser();
        LessonProgressResponse response = progressService.markLessonComplete(
                user.userId(), lessonId, request.getCourseId(), authorizationHeader);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/progress/courses/{courseId}
     * Returns the progress summary for a specific course.
     */
    @GetMapping("/courses/{courseId}")
    public ResponseEntity<CourseProgressResponse> getCourseProgress(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader) {

        AuthenticatedUser user = getAuthenticatedUser();
        CourseProgressResponse response = progressService.getCourseProgress(
                user.userId(), courseId, authorizationHeader);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/progress/me
     * Returns a dashboard of progress across all courses the student has tracked.
     */
    @GetMapping("/me")
    public ResponseEntity<List<CourseProgressResponse>> getMyDashboard(
            @RequestHeader("Authorization") String authorizationHeader) {

        AuthenticatedUser user = getAuthenticatedUser();
        List<CourseProgressResponse> response = progressService.getMyDashboard(
                user.userId(), authorizationHeader);
        return ResponseEntity.ok(response);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private AuthenticatedUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new ForbiddenOperationException("Authentication is required");
    }
}
