package com.ashique.progress_service.client.client_dto;

import java.util.UUID;

public record InternalLessonExistsResponse(
        Boolean exists,
        UUID courseId
) {
}
