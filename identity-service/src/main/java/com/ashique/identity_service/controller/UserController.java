package com.ashique.identity_service.controller;

import com.ashique.identity_service.dto.UserProfileResponse;
import com.ashique.identity_service.entity.User;
import com.ashique.identity_service.exception.ResourceNotFoundException;
import com.ashique.identity_service.repository.UserRepository;
import com.ashique.learnsphere.jwt.AuthenticatedUser;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/users/me")
    public ResponseEntity<UserProfileResponse> me() {
        UsernamePasswordAuthenticationToken auth =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder
                        .getContext().getAuthentication();
        UUID userId = ((AuthenticatedUser) auth.getPrincipal()).userId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        UserProfileResponse response = UserProfileResponse.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }
}
