package com.ashique.enrollment_service.client.dto;

public record InternalCourseExistsResponse(
        Boolean exists,
        CourseStatus status
) {
}
