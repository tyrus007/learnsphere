package com.ashique.enrollment_service.client;

import com.ashique.enrollment_service.client.client_dto.InternalNotificationRequest;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Component
public class NotificationServiceClient {

    private static final String NOTIFICATION_URI = "/api/internal/notifications";

    private final RestClient restClient;

    public NotificationServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${notification-service.url}") String notificationServiceUrl) {
        this.restClient = restClientBuilder
                .baseUrl(notificationServiceUrl)
                .build();
    }

    public void sendEnrollmentNotification(UUID userId, UUID courseId, String authorizationHeader) {
        InternalNotificationRequest request = new InternalNotificationRequest(
                userId,
                "ENROLLMENT_CONFIRMATION",
                "Enrollment Successful",
                "You have successfully enrolled in course: " + courseId
        );

        restClient.post()
                .uri(NOTIFICATION_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .headers(headers -> applyAuthorizationHeader(headers, authorizationHeader))
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    private void applyAuthorizationHeader(HttpHeaders headers, String authorizationHeader) {
        if (StringUtils.hasText(authorizationHeader)) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
    }
}
