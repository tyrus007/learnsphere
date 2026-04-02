package com.ashique.enrollment_service.client.client_dto;

import java.util.UUID;

public record InternalCourseSummaryResponse(
        Boolean exists,
        CourseStatus status,
        UUID instructorId
) {
}
