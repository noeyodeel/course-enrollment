package com.example.enrollment.web;

import com.example.enrollment.service.EnrollmentService;
import com.example.enrollment.web.dto.EnrollmentCreateRequest;
import com.example.enrollment.web.dto.EnrollmentResponse;
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
    public EnrollmentResponse enroll(
            @RequestHeader("X-USER-ID") Long userId,
            @Valid @RequestBody EnrollmentCreateRequest request
    ) {
        return enrollmentService.enroll(userId, request.courseId());
    }

    @PatchMapping("/{enrollmentId}/confirm-payment")
    public EnrollmentResponse confirmPayment(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long enrollmentId
    ) {
        return enrollmentService.confirmPayment(userId, enrollmentId);
    }

    @PatchMapping("/{enrollmentId}/cancel")
    public EnrollmentResponse cancel(
            @RequestHeader("X-USER-ID") Long userId,
            @PathVariable Long enrollmentId
    ) {
        return enrollmentService.cancel(userId, enrollmentId);
    }

    @GetMapping("/me")
    public Page<EnrollmentResponse> myEnrollments(@RequestHeader("X-USER-ID") Long userId, Pageable pageable) {
        return enrollmentService.myEnrollments(userId, pageable);
    }
}
