package com.ashique.enrollment_service.client;

import com.ashique.enrollment_service.client.client_dto.InternalCourseExistsResponse;
import com.ashique.enrollment_service.client.client_dto.InternalCourseLessonCountResponse;
import com.ashique.enrollment_service.client.client_dto.InternalCourseSummaryResponse;
import com.ashique.enrollment_service.exception.CourseServiceClientException;
import com.ashique.enrollment_service.exception.ForbiddenOperationException;
import com.ashique.enrollment_service.exception.ResourceNotFoundException;
import com.ashique.enrollment_service.exception.ServiceUnavailableException;
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

    private static final String COURSE_EXISTS_URI = "/api/internal/courses/{courseId}/exists";

    private static final String COURSE_SUMMARY_URI = "/api/internal/courses/{courseId}/summary";

    private static final String COURSE_LESSON_COUNT_URI = "/api/internal/courses/{courseId}/lesson-count";

    private final RestClient restClient;

    public CourseServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${course-service.url}") String courseServiceUrl) {
        this.restClient = restClientBuilder
                .baseUrl(courseServiceUrl)
                .build();
    }

    public InternalCourseExistsResponse getCourseExists(UUID courseId, String authorizationHeader) {
        return executeGet(
                COURSE_EXISTS_URI,
                courseId,
                authorizationHeader,
                InternalCourseExistsResponse.class,
                "Course not found with id: " + courseId);
    }

    public InternalCourseLessonCountResponse getLessonCount(UUID courseId, String authorizationHeader) {
        return executeGet(
                COURSE_LESSON_COUNT_URI,
                courseId,
                authorizationHeader,
                InternalCourseLessonCountResponse.class,
                "Course not found with id: " + courseId);
    }

    public InternalCourseSummaryResponse getCourseSummary(UUID courseId, String authorizationHeader) {
        return executeGet(
                COURSE_SUMMARY_URI,
                courseId,
                authorizationHeader,
                InternalCourseSummaryResponse.class,
                "Course not found with id: " + courseId);
    }

    private <T> T executeGet(
            String uri,
            UUID courseId,
            String authorizationHeader,
            Class<T> responseType,
            String notFoundMessage) {
        try {
            T response = restClient.get()
                    .uri(uri, courseId)
                    .headers(headers -> applyAuthorizationHeader(headers, authorizationHeader))
                    .retrieve()    // execute request and processes response for body
                    .body(responseType);   // converts json response into target DTO(responseType)


            if (response == null) {
                throw new CourseServiceClientException(
                        "Course Service returned an empty response for course id: " + courseId,
                        null);
            }

            return response;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException(notFoundMessage, ex);
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new ForbiddenOperationException("You do not have access to this course", ex);
        } catch (ResourceAccessException ex) {
            throw new ServiceUnavailableException("Course Service is unavailable", ex);
        } catch (RestClientException ex) {
            throw new CourseServiceClientException("Course Service call failed for course id: " + courseId, ex);
        }
    }

    private void applyAuthorizationHeader(HttpHeaders headers, String authorizationHeader) {
        if (StringUtils.hasText(authorizationHeader)) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
    }
}
