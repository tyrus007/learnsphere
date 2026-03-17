package com.ashique.identity_service.dto;

import com.ashique.identity_service.entity.Role;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private UUID userId;
    private String fullName;
    private String email;
    private Role role;
    private Instant createdAt;
}
