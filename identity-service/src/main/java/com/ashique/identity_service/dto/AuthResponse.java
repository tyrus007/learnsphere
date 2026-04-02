package com.ashique.identity_service.dto;

import com.ashique.identity_service.entity.Role;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class AuthResponse {
    String token;
    UUID userId;
    String email;
    Role role;
}