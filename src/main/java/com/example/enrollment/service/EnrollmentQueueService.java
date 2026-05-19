package com.example.enrollment.service;

import com.example.enrollment.domain.Course;
import com.example.enrollment.domain.CourseStatus;
import com.example.enrollment.domain.EnrollmentQueue;
import com.example.enrollment.domain.EnrollmentQueueStatus;
import com.example.enrollment.domain.EnrollmentStatus;
import com.example.enrollment.repository.CourseRepository;
import com.example.enrollment.repository.EnrollmentQueueRepository;
import com.example.enrollment.repository.EnrollmentRepository;
import com.example.enrollment.web.dto.EnrollmentQueueResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentQueueService {

    private static final int READY_EXPIRATION_MINUTES = 10;
    private static final List<EnrollmentStatus> ACTIVE_ENROLLMENT_STATUSES =
            List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED);
    private static final List<EnrollmentQueueStatus> ACTIVE_QUEUE_STATUSES =
            List.of(EnrollmentQueueStatus.WAITING, EnrollmentQueueStatus.READY);

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentQueueRepository enrollmentQueueRepository;
    private final Clock clock;

    public EnrollmentQueueService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            EnrollmentQueueRepository enrollmentQueueRepository,
            Clock clock
    ) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentQueueRepository = enrollmentQueueRepository;
        this.clock = clock;
    }

    @Transactional
    public EnrollmentQueueResponse enter(Long userId, Long courseId) {
        Course course = lockOpenCourse(courseId);
        LocalDateTime now = LocalDateTime.now(clock);
        expireReadyQueues(courseId, now);
        promoteWaitingQueues(courseId, now);

        if (enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(courseId, userId, ACTIVE_ENROLLMENT_STATUSES)) {
            throw new ApiException(HttpStatus.CONFLICT, "User already has an active enrollment for this course.");
        }
        if (enrollmentQueueRepository.existsByCourseIdAndUserIdAndStatusIn(courseId, userId, ACTIVE_QUEUE_STATUSES)) {
            throw new ApiException(HttpStatus.CONFLICT, "User already has an active queue for this course.");
        }

        EnrollmentQueue queue = new EnrollmentQueue(course, userId);
        if (availableReadySlots(course) > 0) {
            queue.markReady(now, now.plusMinutes(READY_EXPIRATION_MINUTES));
        }

        EnrollmentQueue saved = enrollmentQueueRepository.save(queue);
        return toResponse(saved);
    }

    @Transactional
    public EnrollmentQueueResponse getMine(Long userId, Long courseId) {
        lockCourse(courseId);
        LocalDateTime now = LocalDateTime.now(clock);
        expireReadyQueues(courseId, now);
        promoteWaitingQueues(courseId, now);

        EnrollmentQueue queue = enrollmentQueueRepository
                .findFirstByCourseIdAndUserIdAndStatusInOrderByCreatedAtDesc(
                        courseId,
                        userId,
                        List.of(
                                EnrollmentQueueStatus.WAITING,
                                EnrollmentQueueStatus.READY,
                                EnrollmentQueueStatus.COMPLETED,
                                EnrollmentQueueStatus.EXPIRED,
                                EnrollmentQueueStatus.CANCELLED
                        )
                )
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Queue not found."));
        return toResponse(queue);
    }

    @Transactional
    public EnrollmentQueueResponse cancel(Long userId, Long courseId) {
        lockCourse(courseId);
        LocalDateTime now = LocalDateTime.now(clock);
        EnrollmentQueue queue = enrollmentQueueRepository
                .findFirstByCourseIdAndUserIdAndStatusInOrderByCreatedAtDesc(courseId, userId, ACTIVE_QUEUE_STATUSES)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Active queue not found."));
        queue.cancel(now);
        promoteWaitingQueues(courseId, now);
        return toResponse(queue);
    }

    @Transactional
    public void promoteWaitingQueues(Long courseId) {
        courseRepository.findByIdForUpdate(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found."));
        promoteWaitingQueues(courseId, LocalDateTime.now(clock));
    }

    public long positionOf(EnrollmentQueue queue) {
        if (queue.getStatus() == EnrollmentQueueStatus.READY
                || queue.getStatus() == EnrollmentQueueStatus.COMPLETED) {
            return 0;
        }
        if (queue.getStatus() != EnrollmentQueueStatus.WAITING) {
            return -1;
        }
        return enrollmentQueueRepository.countByCourseIdAndStatusAndIdLessThan(
                queue.getCourse().getId(),
                EnrollmentQueueStatus.WAITING,
                queue.getId()
        ) + 1;
    }

    private Course lockOpenCourse(Long courseId) {
        Course course = lockCourse(courseId);
        if (course.getStatus() != CourseStatus.OPEN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only open courses can use enrollment queue.");
        }
        return course;
    }

    private Course lockCourse(Long courseId) {
        return courseRepository.findByIdForUpdate(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found."));
    }

    private void expireReadyQueues(Long courseId, LocalDateTime now) {
        enrollmentQueueRepository
                .findByCourseIdAndStatusAndExpiresAtBefore(courseId, EnrollmentQueueStatus.READY, now)
                .forEach(EnrollmentQueue::expire);
    }

    private void promoteWaitingQueues(Long courseId, LocalDateTime now) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found."));
        if (course.getStatus() != CourseStatus.OPEN) {
            return;
        }
        long availableSlots = availableReadySlots(course);
        if (availableSlots <= 0) {
            return;
        }

        List<EnrollmentQueue> waitingQueues = enrollmentQueueRepository
                .findByCourseIdAndStatusOrderByIdAsc(courseId, EnrollmentQueueStatus.WAITING);
        waitingQueues.stream()
                .limit(availableSlots)
                .forEach(queue -> queue.markReady(now, now.plusMinutes(READY_EXPIRATION_MINUTES)));
    }

    private long availableReadySlots(Course course) {
        long activeEnrollments = enrollmentRepository.countByCourseIdAndStatusIn(
                course.getId(),
                ACTIVE_ENROLLMENT_STATUSES
        );
        long readyQueues = enrollmentQueueRepository.countByCourseIdAndStatus(
                course.getId(),
                EnrollmentQueueStatus.READY
        );
        return course.getCapacity() - activeEnrollments - readyQueues;
    }

    private EnrollmentQueueResponse toResponse(EnrollmentQueue queue) {
        return EnrollmentQueueResponse.from(queue, positionOf(queue));
    }
}
