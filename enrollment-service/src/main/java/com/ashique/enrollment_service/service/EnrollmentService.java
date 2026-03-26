package com.ashique.enrollment_service.service;

import com.ashique.enrollment_service.client.CourseServiceClient;
import com.ashique.enrollment_service.client.client_dto.CourseStatus;
import com.ashique.enrollment_service.client.client_dto.InternalCourseSummaryResponse;
import com.ashique.enrollment_service.dto.EnrollRequest;
import com.ashique.enrollment_service.dto.EnrollmentResponse;
import com.ashique.enrollment_service.entity.Enrollment;
import com.ashique.enrollment_service.entity.EnrollmentStatus;
import com.ashique.enrollment_service.exception.DuplicateResourceException;
import com.ashique.enrollment_service.exception.ForbiddenOperationException;
import com.ashique.enrollment_service.exception.InvalidRequestException;
import com.ashique.enrollment_service.repository.EnrollmentRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseServiceClient courseServiceClient;

    @Transactional
    public EnrollmentResponse enroll(UUID userId, EnrollRequest request, String authorizationHeader) {
        InternalCourseSummaryResponse course = courseServiceClient.getCourseSummary(
                request.getCourseId(),
                authorizationHeader);

        if (!Boolean.TRUE.equals(course.exists()) || course.status() != CourseStatus.PUBLISHED) {
            throw new InvalidRequestException("Course is not available for enrollment");
        }

        if (isEnrolled(userId, request.getCourseId())) {
            throw new DuplicateResourceException("Student is already enrolled in this course");
        }

        Enrollment enrollment = enrollmentRepository.save(Enrollment.builder()
                .studentId(userId)
                .courseId(request.getCourseId())
                .status(EnrollmentStatus.ENROLLED)
                .build());

        return toResponse(enrollment);
    }

    public List<EnrollmentResponse> getMyEnrollments(UUID userId) {
        return enrollmentRepository.findAllByStudentIdOrderByEnrolledAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<EnrollmentResponse> getEnrollmentsForCourse(   // this service only be called by instructor
            UUID courseId,
            UUID userId,
            String authorizationHeader) {
        InternalCourseSummaryResponse course = courseServiceClient.getCourseSummary(courseId, authorizationHeader);

        if (!Boolean.TRUE.equals(course.exists())) {
            throw new InvalidRequestException("Course is not available");
        }

        if (!userId.equals(course.instructorId())) {
            throw new ForbiddenOperationException("You do not have access to this course");
        }

        return enrollmentRepository.findAllByCourseIdOrderByEnrolledAtDesc(courseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public boolean isEnrolled(UUID studentId, UUID courseId) {
        return enrollmentRepository.existsByStudentIdAndCourseId(studentId, courseId);
    }

    private EnrollmentResponse toResponse(Enrollment enrollment) {
        return EnrollmentResponse.builder()
                .id(enrollment.getId())
                .studentId(enrollment.getStudentId())
                .courseId(enrollment.getCourseId())
                .status(enrollment.getStatus())
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }
}
