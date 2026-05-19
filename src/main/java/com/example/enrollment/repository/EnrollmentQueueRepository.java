package com.example.enrollment.repository;

import com.example.enrollment.domain.EnrollmentQueue;
import com.example.enrollment.domain.EnrollmentQueueStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentQueueRepository extends JpaRepository<EnrollmentQueue, Long> {

    long countByCourseIdAndStatus(Long courseId, EnrollmentQueueStatus status);

    long countByCourseIdAndStatusAndIdLessThan(Long courseId, EnrollmentQueueStatus status, Long id);

    boolean existsByCourseIdAndUserIdAndStatusIn(
            Long courseId,
            Long userId,
            Collection<EnrollmentQueueStatus> statuses
    );

    Optional<EnrollmentQueue> findByCourseIdAndUserIdAndStatus(
            Long courseId,
            Long userId,
            EnrollmentQueueStatus status
    );

    Optional<EnrollmentQueue> findFirstByCourseIdAndUserIdAndStatusInOrderByCreatedAtDesc(
            Long courseId,
            Long userId,
            Collection<EnrollmentQueueStatus> statuses
    );

    List<EnrollmentQueue> findByCourseIdAndStatusOrderByIdAsc(Long courseId, EnrollmentQueueStatus status);

    List<EnrollmentQueue> findByCourseIdAndStatusAndExpiresAtBefore(
            Long courseId,
            EnrollmentQueueStatus status,
            LocalDateTime now
    );
}
