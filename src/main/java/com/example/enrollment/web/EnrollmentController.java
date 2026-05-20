package com.example.enrollment.web;

import com.example.enrollment.service.EnrollmentService;
import com.example.enrollment.web.dto.EnrollmentCreateRequest;
import com.example.enrollment.web.dto.EnrollmentResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "수강 신청", description = "READY 상태의 대기열을 가진 사용자가 실제 수강 신청을 생성합니다. 생성 상태는 PENDING입니다.")
    public EnrollmentResponse enroll(
            @RequestHeader("X-USER-ID") Long userId,
            @Valid @RequestBody EnrollmentCreateRequest request
    ) {
        return enrollmentService.enroll(userId, request.courseId());
    }

    @PatchMapping("/{enrollmentId}/confirm-payment")
    @Operation(summary = "결제 확정", description = "PENDING 상태의 수강 신청을 CONFIRMED 상태로 변경합니다.")
    public EnrollmentResponse confirmPayment(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long enrollmentId
    ) {
        return enrollmentService.confirmPayment(userId, enrollmentId);
    }

    @PatchMapping("/{enrollmentId}/cancel")
    @Operation(summary = "수강 취소", description = "수강 신청을 CANCELLED 상태로 변경합니다. CONFIRMED 상태는 결제 확정 후 7일 이내에만 취소할 수 있습니다.")
    public EnrollmentResponse cancel(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long enrollmentId
    ) {
        return enrollmentService.cancel(userId, enrollmentId);
    }

    @GetMapping("/me")
    @Operation(summary = "내 수강 신청 목록 조회", description = "사용자의 수강 신청 목록을 페이지네이션으로 조회합니다.")
    public Page<EnrollmentResponse> myEnrollments(@RequestHeader("X-USER-ID") Long userId, Pageable pageable) {
        return enrollmentService.myEnrollments(userId, pageable);
    }
}
