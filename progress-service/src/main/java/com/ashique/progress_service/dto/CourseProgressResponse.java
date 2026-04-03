package com.ashique.progress_service.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class CourseProgressResponse {
    UUID courseId;
    long completedLessons;
    long totalLessons;
    double percentage;
}
