package com.ashique.courseservice.dto;

import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class ModuleResponse {
    UUID id;
    UUID courseId;
    String title;
    Integer position;
    List<LessonResponse> lessons;
}
