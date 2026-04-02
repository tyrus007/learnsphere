package com.ashique.courseservice.dto;

import com.ashique.courseservice.entity.CourseLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class UpdateCourseRequest {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    String title;

    String description;

    @NotNull(message = "Level is required")
    CourseLevel level;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    String category;
}
