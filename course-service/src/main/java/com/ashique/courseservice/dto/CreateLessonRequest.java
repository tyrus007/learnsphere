package com.ashique.courseservice.dto;

import com.ashique.courseservice.entity.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class CreateLessonRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    String title;

    @NotNull(message = "Content type is required")
    ContentType contentType;

    String contentUrlOrBody;

    @Builder.Default
    Boolean isPreview = false;
}
