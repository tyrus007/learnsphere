package com.ashique.enrollment_service.client.client_dto;

import java.util.UUID;

public record InternalNotificationRequest(
        UUID userId,
        String type,
        String title,
        String message
) {
}
