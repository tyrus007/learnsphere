package com.ashique.enrollment_service.dto;

import com.ashique.enrollment_service.entity.EnrollmentStatus;
import java.time.Instant;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class EnrollmentResponse {
    UUID id;
    UUID studentId;
    UUID courseId;
    EnrollmentStatus status;
    Instant enrolledAt;
}
