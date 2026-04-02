package com.ashique.enrollment_service.client.client_dto;

public record InternalCourseExistsResponse(
        Boolean exists,
        CourseStatus status
) {
}
