package com.example.enrollment.web.dto;

import com.example.enrollment.domain.EnrollmentQueue;
import com.example.enrollment.domain.EnrollmentQueueStatus;
import java.time.LocalDateTime;

public record EnrollmentQueueResponse(
        Long id,
        Long courseId,
        Long userId,
        EnrollmentQueueStatus status,
        long position,
        LocalDateTime readyAt,
        LocalDateTime expiresAt,
        LocalDateTime createdAt
) {

    public static EnrollmentQueueResponse from(EnrollmentQueue queue, long position) {
        return new EnrollmentQueueResponse(
                queue.getId(),
                queue.getCourse().getId(),
                queue.getUserId(),
                queue.getStatus(),
                position,
                queue.getReadyAt(),
                queue.getExpiresAt(),
                queue.getCreatedAt()
        );
    }
}
