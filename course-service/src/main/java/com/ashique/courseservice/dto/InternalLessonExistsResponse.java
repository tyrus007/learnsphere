package com.ashique.courseservice.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class InternalLessonExistsResponse {
    Boolean exists;
    UUID courseId;
}
