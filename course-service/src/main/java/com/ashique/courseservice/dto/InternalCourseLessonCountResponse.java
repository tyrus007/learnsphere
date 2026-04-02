package com.ashique.courseservice.dto;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class InternalCourseLessonCountResponse {
    Long lessonCount;
}
