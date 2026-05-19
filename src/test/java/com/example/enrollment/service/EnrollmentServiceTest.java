package com.example.enrollment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.enrollment.domain.Course;
import com.example.enrollment.domain.CourseStatus;
import com.example.enrollment.domain.EnrollmentQueueStatus;
import com.example.enrollment.domain.EnrollmentStatus;
import com.example.enrollment.repository.CourseRepository;
import com.example.enrollment.repository.EnrollmentRepository;
import com.example.enrollment.web.dto.EnrollmentQueueResponse;
import com.example.enrollment.web.dto.EnrollmentResponse;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EnrollmentServiceTest {

    @Autowired
    EnrollmentService enrollmentService;

    @Autowired
    EnrollmentQueueService enrollmentQueueService;

    @Autowired
    CourseRepository courseRepository;

    @Autowired
    EnrollmentRepository enrollmentRepository;

    @Autowired
    MutableClock mutableClock;

    @Test
    void draftCourseCannotBeEnrolled() {
        // DRAFT 상태의 강의는 아직 모집 전이므로 수강 신청을 막는다.
        Course course = saveCourse(1);

        assertThatThrownBy(() -> enrollmentService.enroll(10L, course.getId()))
                .isInstanceOf(ApiException.class)
                .extracting("status")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void enrollmentIsRejectedWhenCapacityIsExceeded() {
        // 남은 정원이 없으면 새 사용자는 WAITING 상태가 되고, 실제 수강 신청은 할 수 없다.
        Course course = saveOpenCourse(1);

        enrollmentQueueService.enter(10L, course.getId());
        enrollmentService.enroll(10L, course.getId());

        enrollmentQueueService.enter(11L, course.getId());
        assertThatThrownBy(() -> enrollmentService.enroll(11L, course.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Ready queue");
    }

    @Test
    void pendingEnrollmentCanBeConfirmedByPayment() {
        // READY 사용자가 신청하면 PENDING이 되고, 결제 확정 후 CONFIRMED가 된다.
        Course course = saveOpenCourse(2);
        enrollmentQueueService.enter(10L, course.getId());
        EnrollmentResponse pending = enrollmentService.enroll(10L, course.getId());

        EnrollmentResponse confirmed = enrollmentService.confirmPayment(10L, pending.id());

        assertThat(confirmed.status()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(confirmed.confirmedAt()).isNotNull();
    }

    @Test
    void confirmedEnrollmentCannotBeCancelledAfterSevenDays() {
        // 결제 확정 후 7일이 지난 CONFIRMED 신청은 취소할 수 없다.
        Course course = saveOpenCourse(2);
        enrollmentQueueService.enter(10L, course.getId());
        EnrollmentResponse pending = enrollmentService.enroll(10L, course.getId());
        mutableClock.setInstant("2026-05-01T00:00:00Z");
        EnrollmentResponse confirmed = enrollmentService.confirmPayment(10L, pending.id());
        mutableClock.setInstant("2026-05-17T00:00:00Z");

        assertThatThrownBy(() -> enrollmentService.cancel(10L, confirmed.id()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("7 days");
    }

    @Test
    void queueGivesReadyOnlyAsManyAsRemainingCapacity() {
        // 대기열은 남은 정원만큼만 READY를 발급하고, 초과 인원은 WAITING으로 둔다.
        Course course = saveOpenCourse(2);

        EnrollmentQueueResponse first = enrollmentQueueService.enter(10L, course.getId());
        EnrollmentQueueResponse second = enrollmentQueueService.enter(11L, course.getId());
        EnrollmentQueueResponse third = enrollmentQueueService.enter(12L, course.getId());

        assertThat(first.status()).isEqualTo(EnrollmentQueueStatus.READY);
        assertThat(second.status()).isEqualTo(EnrollmentQueueStatus.READY);
        assertThat(third.status()).isEqualTo(EnrollmentQueueStatus.WAITING);
        assertThat(third.position()).isEqualTo(1);
    }

    @Test
    void queueSubtractsAlreadyActiveEnrollmentsFromReadySlots() {
        // 정원 100명 중 95명이 이미 신청했다면 10명 접속 시 5명만 READY가 되는 규칙을 작은 숫자로 검증한다.
        Course course = saveOpenCourse(5);
        createPendingEnrollment(10L, course.getId());
        createPendingEnrollment(11L, course.getId());
        createPendingEnrollment(12L, course.getId());

        EnrollmentQueueResponse first = enrollmentQueueService.enter(20L, course.getId());
        EnrollmentQueueResponse second = enrollmentQueueService.enter(21L, course.getId());
        EnrollmentQueueResponse third = enrollmentQueueService.enter(22L, course.getId());

        assertThat(first.status()).isEqualTo(EnrollmentQueueStatus.READY);
        assertThat(second.status()).isEqualTo(EnrollmentQueueStatus.READY);
        assertThat(third.status()).isEqualTo(EnrollmentQueueStatus.WAITING);
        assertThat(third.position()).isEqualTo(1);
    }

    @Test
    void readyQueueIsRequiredBeforeEnrollment() {
        // 수강 신청은 티켓팅식 대기열에서 READY 상태를 받은 사용자만 가능하다.
        Course course = saveOpenCourse(1);

        assertThatThrownBy(() -> enrollmentService.enroll(10L, course.getId()))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Ready queue");
    }

    @Test
    void waitingQueueIsPromotedWhenEnrollmentIsCancelled() {
        // 수강 취소로 자리가 생기면 가장 먼저 기다리던 WAITING 사용자를 READY로 승격한다.
        Course course = saveOpenCourse(1);
        enrollmentQueueService.enter(10L, course.getId());
        EnrollmentResponse enrollment = enrollmentService.enroll(10L, course.getId());
        EnrollmentQueueResponse waiting = enrollmentQueueService.enter(11L, course.getId());

        assertThat(waiting.status()).isEqualTo(EnrollmentQueueStatus.WAITING);

        enrollmentService.cancel(10L, enrollment.id());
        EnrollmentQueueResponse promoted = enrollmentQueueService.getMine(11L, course.getId());

        assertThat(promoted.status()).isEqualTo(EnrollmentQueueStatus.READY);
        assertThat(promoted.position()).isZero();
    }

    private Course saveOpenCourse(int capacity) {
        // 테스트에서 바로 신청 가능한 강의를 만들기 위한 헬퍼.
        Course course = saveCourse(capacity);
        course.changeStatus(CourseStatus.OPEN);
        return course;
    }

    private Course saveCourse(int capacity) {
        // 기본 상태는 DRAFT이며, 테스트별로 필요한 경우 OPEN으로 변경한다.
        return courseRepository.save(new Course(
                1L,
                "Spring Boot Basic",
                "Learn Spring Boot with practical examples.",
                BigDecimal.valueOf(10000),
                capacity,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 7, 1)
        ));
    }

    private EnrollmentResponse createPendingEnrollment(Long userId, Long courseId) {
        // 기존 신청자가 이미 있는 상황을 만들기 위해 정상 대기열 흐름을 거쳐 PENDING 신청을 생성한다.
        enrollmentQueueService.enter(userId, courseId);
        return enrollmentService.enroll(userId, courseId);
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        MutableClock mutableClock() {
            // 시간 기반 취소 정책을 안정적으로 검증하기 위해 테스트용 Clock을 고정한다.
            return new MutableClock(Instant.parse("2026-05-17T00:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }

    static class MutableClock extends Clock {

        private final AtomicReference<Instant> instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = new AtomicReference<>(instant);
            this.zone = zone;
        }

        void setInstant(String instant) {
            // 테스트 중 결제 시점과 취소 시점을 명시적으로 이동한다.
            this.instant.set(Instant.parse(instant));
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant.get(), zone);
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
