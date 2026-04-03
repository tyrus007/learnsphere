package com.ashique.progress_service.client;

import com.ashique.progress_service.exception.ServiceClientException;
import com.ashique.progress_service.exception.ServiceUnavailableException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class EnrollmentServiceClient {

    private static final String ENROLLMENT_CHECK_URI =
            "/api/enrollments/check?studentId={studentId}&courseId={courseId}";

    private final RestClient restClient;

    public EnrollmentServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${enrollment-service.url}") String enrollmentServiceUrl) {
        this.restClient = restClientBuilder
                .baseUrl(enrollmentServiceUrl)
                .build();
    }

    /**
     * Checks whether a student is enrolled in a course by calling the
     * Enrollment Service's check endpoint.
     *
     * @param studentId the student's UUID
     * @param courseId  the course's UUID
     * @param authorizationHeader the Authorization header value to forward
     * @return true if the student is enrolled, false otherwise
     */
    public boolean isEnrolled(UUID studentId, UUID courseId, String authorizationHeader) {
        try {
            Boolean result = restClient.get()
                    .uri(ENROLLMENT_CHECK_URI, studentId, courseId)
                    .headers(headers -> applyAuthorizationHeader(headers, authorizationHeader))
                    .retrieve()
                    .body(Boolean.class);

            return Boolean.TRUE.equals(result);
        } catch (ResourceAccessException ex) {
            throw new ServiceUnavailableException("Enrollment Service is unavailable", ex);
        } catch (RestClientException ex) {
            throw new ServiceClientException(
                    "Enrollment Service call failed for studentId: " + studentId + ", courseId: " + courseId, ex);
        }
    }

    private void applyAuthorizationHeader(HttpHeaders headers, String authorizationHeader) {
        if (StringUtils.hasText(authorizationHeader)) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
    }
}
