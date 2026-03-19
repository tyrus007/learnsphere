package com.ashique.courseservice.dto;

import com.ashique.courseservice.entity.CourseStatus;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class InternalCourseExistsResponse {
    Boolean exists;
    CourseStatus status;
}
