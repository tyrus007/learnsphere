package com.ashique.courseservice.dto;

import com.ashique.courseservice.entity.CourseLevel;
import com.ashique.courseservice.entity.CourseStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class CourseDetailResponse {
    UUID id;
    UUID instructorId;
    String title;
    String description;
    CourseLevel level;
    String category;
    CourseStatus status;
    Instant createdAt;
    List<ModuleResponse> modules;
}
