package com.ashique.progress_service.client;

import com.ashique.progress_service.client.client_dto.InternalCourseLessonCountResponse;
import com.ashique.progress_service.client.client_dto.InternalLessonExistsResponse;
import com.ashique.progress_service.exception.ForbiddenOperationException;
import com.ashique.progress_service.exception.ResourceNotFoundException;
import com.ashique.progress_service.exception.ServiceClientException;
import com.ashique.progress_service.exception.ServiceUnavailableException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class CourseServiceClient {

    private static final String LESSON_EXISTS_URI = "/api/internal/lessons/{lessonId}/exists";

    private static final String COURSE_LESSON_COUNT_URI = "/api/internal/courses/{courseId}/lesson-count";

    private final RestClient restClient;

    public CourseServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${course-service.url}") String courseServiceUrl) {
        this.restClient = restClientBuilder
                .baseUrl(courseServiceUrl)
                .build();
    }

    public InternalLessonExistsResponse getLessonExists(UUID lessonId, String authorizationHeader) {
        return executeGet(
                LESSON_EXISTS_URI,
                lessonId,
                authorizationHeader,
                InternalLessonExistsResponse.class,
                "Lesson not found with id: " + lessonId);
    }

    public InternalCourseLessonCountResponse getLessonCount(UUID courseId, String authorizationHeader) {
        return executeGet(
                COURSE_LESSON_COUNT_URI,
                courseId,
                authorizationHeader,
                InternalCourseLessonCountResponse.class,
                "Course not found with id: " + courseId);
    }

    private <T> T executeGet(
            String uri,
            UUID id,
            String authorizationHeader,
            Class<T> responseType,
            String notFoundMessage) {
        try {
            T response = restClient.get()
                    .uri(uri, id)
                    .headers(headers -> applyAuthorizationHeader(headers, authorizationHeader))
                    .retrieve()
                    .body(responseType);

            if (response == null) {
                throw new ServiceClientException(
                        "Course Service returned an empty response for id: " + id,
                        null);
            }

            return response;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException(notFoundMessage, ex);
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new ForbiddenOperationException("You do not have access to this resource", ex);
        } catch (ResourceAccessException ex) {
            throw new ServiceUnavailableException("Course Service is unavailable", ex);
        } catch (RestClientException ex) {
            throw new ServiceClientException("Course Service call failed for id: " + id, ex);
        }
    }

    private void applyAuthorizationHeader(HttpHeaders headers, String authorizationHeader) {
        if (StringUtils.hasText(authorizationHeader)) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
    }
}
