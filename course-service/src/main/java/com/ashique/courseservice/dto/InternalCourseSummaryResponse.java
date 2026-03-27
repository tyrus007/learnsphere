package com.ashique.courseservice.dto;

import com.ashique.courseservice.entity.CourseStatus;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
public class InternalCourseSummaryResponse {
    Boolean exists;
    CourseStatus status;
    UUID instructorId;
}
