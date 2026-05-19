package com.example.enrollment.web.dto;

import com.example.enrollment.domain.Enrollment;
import com.example.enrollment.domain.EnrollmentStatus;
import java.time.LocalDateTime;

public record EnrollmentResponse(
        Long id,
        Long courseId,
        Long userId,
        EnrollmentStatus status,
        LocalDateTime createdAt,
        LocalDateTime confirmedAt,
        LocalDateTime cancelledAt
) {

    public static EnrollmentResponse from(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getCourse().getId(),
                enrollment.getUserId(),
                enrollment.getStatus(),
                enrollment.getCreatedAt(),
                enrollment.getConfirmedAt(),
                enrollment.getCancelledAt()
        );
    }
}
