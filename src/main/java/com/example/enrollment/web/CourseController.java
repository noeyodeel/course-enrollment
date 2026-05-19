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
    public CourseResponse create(
            @RequestHeader("X-CREATOR-ID") Long creatorId,
            @Valid @RequestBody CourseCreateRequest request
    ) {
        return courseService.create(creatorId, request);
    }

    @PatchMapping("/{courseId}/status")
    public CourseResponse changeStatus(
            @PathVariable Long courseId,
            @RequestHeader("X-CREATOR-ID") Long creatorId,
            @Valid @RequestBody CourseStatusRequest request
    ) {
        return courseService.changeStatus(courseId, creatorId, request.status());
    }

    @GetMapping
    public List<CourseResponse> list(@RequestParam(required = false) CourseStatus status) {
        return courseService.list(status);
    }

    @GetMapping("/{courseId}")
    public CourseResponse detail(@PathVariable Long courseId) {
        return courseService.detail(courseId);
    }

    @GetMapping("/{courseId}/enrollments")
    public List<EnrollmentResponse> courseEnrollments(
            @PathVariable Long courseId,
            @RequestHeader("X-CREATOR-ID") Long creatorId
    ) {
        return enrollmentService.courseEnrollments(creatorId, courseId);
    }

    @PostMapping("/{courseId}/queue")
    @ResponseStatus(HttpStatus.CREATED)
    public EnrollmentQueueResponse enterQueue(
            @PathVariable Long courseId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return enrollmentQueueService.enter(userId, courseId);
    }

    @GetMapping("/{courseId}/queue/me")
    public EnrollmentQueueResponse myQueue(
            @PathVariable Long courseId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return enrollmentQueueService.getMine(userId, courseId);
    }

    @PatchMapping("/{courseId}/queue/cancel")
    public EnrollmentQueueResponse cancelQueue(
            @PathVariable Long courseId,
            @RequestHeader("X-USER-ID") Long userId
    ) {
        return enrollmentQueueService.cancel(userId, courseId);
    }
}
