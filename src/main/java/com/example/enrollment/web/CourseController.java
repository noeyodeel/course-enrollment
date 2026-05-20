package com.example.enrollment.web;

import com.example.enrollment.domain.CourseStatus;
import com.example.enrollment.service.CourseService;
import com.example.enrollment.service.EnrollmentQueueService;
import com.example.enrollment.service.EnrollmentService;
import com.example.enrollment.web.dto.CourseCreateRequest;
import com.example.enrollment.web.dto.CourseResponse;
import com.example.enrollment.web.dto.CourseStatusRequest;
import com.example.enrollment.web.dto.EnrollmentQueueResponse;
import com.example.enrollment.web.dto.EnrollmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classes")
public class CourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final EnrollmentQueueService enrollmentQueueService;

    public CourseController(
            CourseService courseService,
            EnrollmentService enrollmentService,
            EnrollmentQueueService enrollmentQueueService
    ) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.enrollmentQueueService = enrollmentQueueService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "강의 등록", description = "크리에이터가 강의를 등록합니다. 생성된 강의의 기본 상태는 DRAFT입니다.")
    public CourseResponse create(
            @RequestHeader("X-CREATOR-ID") Long creatorId,
            @Valid @RequestBody CourseCreateRequest request
    ) {
        return courseService.create(creatorId, request);
    }

    @PatchMapping("/{courseId}/status")
    @Operation(summary = "강의 상태 변경", description = "강의 생성자가 강의 상태를 DRAFT, OPEN, CLOSED 중 하나로 변경합니다.")
    public CourseResponse changeStatus(
            @PathVariable Long courseId,
            @RequestHeader("X-CREATOR-ID") Long creatorId,
            @Valid @RequestBody CourseStatusRequest request
    ) {
        return courseService.changeStatus(courseId, creatorId, request.status());
    }

    @GetMapping
    @Operation(summary = "강의 목록 조회", description = "강의 목록을 조회합니다. status 파라미터로 상태 필터링이 가능합니다.")
    public List<CourseResponse> list(@RequestParam(required = false) CourseStatus status) {
        return courseService.list(status);
    }

    @GetMapping("/{courseId}")
    @Operation(summary = "강의 상세 조회", description = "강의 상세 정보와 현재 신청 인원(enrolledCount)을 조회합니다.")
    public CourseResponse detail(@PathVariable Long courseId) {
        return courseService.detail(courseId);
    }

    @GetMapping("/{courseId}/enrollments")
    @Operation(summary = "강의별 수강생 목록 조회", description = "강의를 만든 크리에이터가 해당 강의의 활성 수강 신청 목록을 조회합니다.")
    public List<EnrollmentResponse> courseEnrollments(
            @PathVariable Long courseId,
            @RequestHeader("X-CREATOR-ID") Long creatorId
    ) {
        return enrollmentService.courseEnrollments(creatorId, courseId);
    }

    @PostMapping("/{courseId}/queue")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "대기열 진입", description = "사용자가 강의 수강 신청 전 대기열에 진입합니다. 남은 정원이 있으면 READY, 없으면 WAITING 상태가 됩니다.")
    public EnrollmentQueueResponse enterQueue(
            @PathVariable Long courseId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return enrollmentQueueService.enter(userId, courseId);
    }

    @GetMapping("/{courseId}/queue/me")
    @Operation(summary = "내 대기열 상태 조회", description = "사용자의 대기열 상태와 대기 순번을 조회합니다. READY 상태의 position은 0입니다.")
    public EnrollmentQueueResponse myQueue(
            @PathVariable Long courseId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return enrollmentQueueService.getMine(userId, courseId);
    }

    @PatchMapping("/{courseId}/queue/cancel")
    @Operation(summary = "대기열 취소", description = "사용자의 활성 대기열을 취소합니다. 자리가 생기면 다음 WAITING 사용자가 READY로 승격될 수 있습니다.")
    public EnrollmentQueueResponse cancelQueue(
            @PathVariable Long courseId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return enrollmentQueueService.cancel(userId, courseId);
    }
}
