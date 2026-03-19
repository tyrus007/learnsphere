package com.ashique.courseservice.controllers;

import com.ashique.courseservice.dto.CreateLessonRequest;
import com.ashique.courseservice.dto.LessonResponse;
import com.ashique.courseservice.exceptions.ForbiddenOperationException;
import com.ashique.courseservice.services.LessonService;
import com.ashique.learnsphere.jwt.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/modules/{moduleId}/lessons")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @PostMapping
    @PreAuthorize("hasRole('INSTRUCTOR')")
    public ResponseEntity<LessonResponse> createLesson(
            @PathVariable UUID moduleId,
            @Valid @RequestBody CreateLessonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lessonService.createLesson(getAuthenticatedUser().userId(), moduleId, request));
    }

    private AuthenticatedUser getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof UsernamePasswordAuthenticationToken token
                && token.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }

        throw new ForbiddenOperationException("Authentication is required");
    }
}
