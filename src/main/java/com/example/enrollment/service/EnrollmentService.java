package com.example.enrollment.service;

import com.example.enrollment.domain.Course;
import com.example.enrollment.domain.CourseStatus;
import com.example.enrollment.domain.Enrollment;
import com.example.enrollment.domain.EnrollmentQueue;
import com.example.enrollment.domain.EnrollmentQueueStatus;
import com.example.enrollment.domain.EnrollmentStatus;
import com.example.enrollment.repository.CourseRepository;
import com.example.enrollment.repository.EnrollmentQueueRepository;
import com.example.enrollment.repository.EnrollmentRepository;
import com.example.enrollment.web.dto.EnrollmentResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EnrollmentService {

    private static final int CANCELLABLE_DAYS_AFTER_CONFIRMATION = 7;
    private static final List<EnrollmentStatus> ACTIVE_ENROLLMENT_STATUSES =
            List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED);

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentQueueRepository enrollmentQueueRepository;
    private final EnrollmentQueueService enrollmentQueueService;
    private final Clock clock;

    public EnrollmentService(
            CourseRepository courseRepository,
            EnrollmentRepository enrollmentRepository,
            EnrollmentQueueRepository enrollmentQueueRepository,
            EnrollmentQueueService enrollmentQueueService,
            Clock clock
    ) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentQueueRepository = enrollmentQueueRepository;
        this.enrollmentQueueService = enrollmentQueueService;
        this.clock = clock;
    }

    @Transactional
    public EnrollmentResponse enroll(Long userId, Long courseId) {
        Course course = courseRepository.findByIdForUpdate(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found."));
        if (course.getStatus() != CourseStatus.OPEN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only open courses can be enrolled.");
        }
        if (enrollmentRepository.existsByCourseIdAndUserIdAndStatusIn(courseId, userId, ACTIVE_ENROLLMENT_STATUSES)) {
            throw new ApiException(HttpStatus.CONFLICT, "User already has an active enrollment for this course.");
        }
        EnrollmentQueue queue = enrollmentQueueRepository
                .findByCourseIdAndUserIdAndStatus(courseId, userId, EnrollmentQueueStatus.READY)
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT, "Ready queue is required before enrollment."));
        LocalDateTime now = LocalDateTime.now(clock);
        if (queue.isExpired(now)) {
            queue.expire();
            enrollmentQueueService.promoteWaitingQueues(courseId);
            throw new ApiException(HttpStatus.CONFLICT, "Ready queue has expired.");
        }
        long activeCount = enrollmentRepository.countByCourseIdAndStatusIn(courseId, ACTIVE_ENROLLMENT_STATUSES);
        if (activeCount >= course.getCapacity()) {
            throw new ApiException(HttpStatus.CONFLICT, "Course capacity exceeded.");
        }
        Enrollment enrollment = enrollmentRepository.save(new Enrollment(course, userId));
        queue.complete(now);
        return EnrollmentResponse.from(enrollment);
    }

    @Transactional
    public EnrollmentResponse confirmPayment(Long userId, Long enrollmentId) {
        Enrollment enrollment = getOwnedEnrollment(userId, enrollmentId);
        enrollment.confirmPayment(LocalDateTime.now(clock));
        return EnrollmentResponse.from(enrollment);
    }

    @Transactional
    public EnrollmentResponse cancel(Long userId, Long enrollmentId) {
        Enrollment enrollment = getOwnedEnrollment(userId, enrollmentId);
        LocalDateTime now = LocalDateTime.now(clock);
        if (enrollment.getStatus() == EnrollmentStatus.CONFIRMED
                && enrollment.getConfirmedAt().plusDays(CANCELLABLE_DAYS_AFTER_CONFIRMATION).isBefore(now)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Confirmed enrollment can be cancelled within 7 days only.");
        }
        enrollment.cancel(now);
        enrollmentQueueService.promoteWaitingQueues(enrollment.getCourse().getId());
        return EnrollmentResponse.from(enrollment);
    }

    @Transactional(readOnly = true)
    public Page<EnrollmentResponse> myEnrollments(Long userId, Pageable pageable) {
        return enrollmentRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(EnrollmentResponse::from);
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> courseEnrollments(Long creatorId, Long courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found."));
        if (!course.getCreatorId().equals(creatorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only creator can read course enrollments.");
        }
        return enrollmentRepository
                .findByCourseIdAndStatusInOrderByCreatedAtAsc(courseId, ACTIVE_ENROLLMENT_STATUSES)
                .stream()
                .map(EnrollmentResponse::from)
                .toList();
    }

    private Enrollment getOwnedEnrollment(Long userId, Long enrollmentId) {
        return enrollmentRepository.findByIdAndUserId(enrollmentId, userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Enrollment not found."));
    }
}
