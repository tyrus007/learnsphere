package com.ashique.enrollment_service.controller;

import com.ashique.enrollment_service.dto.EnrollRequest;
import com.ashique.enrollment_service.dto.EnrollmentResponse;
import com.ashique.enrollment_service.exception.ForbiddenOperationException;
import com.ashique.enrollment_service.service.EnrollmentService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    /**
     * POST /api/enrollments
     * Enroll the authenticated STUDENT into a course.
     */
    @PostMapping("/enrollments")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<EnrollmentResponse> enroll(
            @Valid @RequestBody EnrollRequest request,
            @RequestHeader("Authorization") String authorizationHeader) {

        AuthenticatedUser user = getAuthenticatedUser();
        EnrollmentResponse response = enrollmentService.enroll(user.userId(), request, authorizationHeader);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/enrollments/me
     * Returns all enrollments belonging to the currently authenticated user.
     */
    @GetMapping("/enrollments/me")
    public ResponseEntity<List<EnrollmentResponse>> getMyEnrollments() {
        AuthenticatedUser user = getAuthenticatedUser();
        return ResponseEntity.ok(enrollmentService.getMyEnrollments(user.userId()));
    }

    /**
     * GET /api/courses/{courseId}/enrollments
     * Returns all students enrolled in a course — INSTRUCTOR only, and must be the course owner.
     */
    @GetMapping("/courses/{courseId}/enrollments")
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<List<EnrollmentResponse>> getEnrollmentsForCourse(
            @PathVariable UUID courseId,
            @RequestHeader("Authorization") String authorizationHeader) {

        AuthenticatedUser user = getAuthenticatedUser();
        return ResponseEntity.ok(
                enrollmentService.getEnrollmentsForCourse(courseId, user.userId(), authorizationHeader));
    }

    /**
     * GET /api/enrollments/check?studentId={}&courseId={}
     * Checks whether a student is enrolled in a course.
     * Access rules:
     *   - The authenticated student can only check their own enrollment.
     *   - An instructor can check any student's enrollment in their course (forwarded to service).
     *   - Any other user querying a studentId that isn't themselves is rejected with 403.
     */
    @GetMapping("/enrollments/check")
    public ResponseEntity<Boolean> checkEnrollment(
            @RequestParam UUID studentId,
            @RequestParam UUID courseId) {

        AuthenticatedUser caller = getAuthenticatedUser();

        boolean isStudent = "ROLE_STUDENT".equals(caller.role()) || "STUDENT".equals(caller.role());
        boolean isInstructor = "ROLE_INSTRUCTOR".equals(caller.role()) || "INSTRUCTOR".equals(caller.role());

        // A student can only check their own enrollment
        if (isStudent && !caller.userId().equals(studentId)) {
            throw new ForbiddenOperationException("You can only check your own enrollment");
        }

        // An instructor is allowed — ownership is already verified in EnrollmentService
        // Any other role that isn't instructor and whose userId != studentId is rejected above
        if (!isStudent && !isInstructor) {
            throw new ForbiddenOperationException("Access is denied");
        }

        return ResponseEntity.ok(enrollmentService.isEnrolled(studentId, courseId));
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
