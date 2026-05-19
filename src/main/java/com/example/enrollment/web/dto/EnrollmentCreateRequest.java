package com.example.enrollment.web.dto;

import jakarta.validation.constraints.NotNull;

public record EnrollmentCreateRequest(@NotNull Long courseId) {
}
