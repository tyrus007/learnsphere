package com.ashique.courseservice.dto;

import com.ashique.courseservice.entity.ContentType;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class LessonResponse {
    UUID id;
    UUID moduleId;
    String title;
    ContentType contentType;
    String contentUrlOrBody;
    Integer position;
    Boolean isPreview;
}
