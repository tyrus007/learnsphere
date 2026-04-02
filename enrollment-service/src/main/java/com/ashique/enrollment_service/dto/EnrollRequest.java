package com.ashique.enrollment_service.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class EnrollRequest {

    @NotNull(message = "Course id is required")
    UUID courseId;
}
