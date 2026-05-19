package com.example.enrollment.web.dto;

import com.example.enrollment.domain.CourseStatus;
import jakarta.validation.constraints.NotNull;

public record CourseStatusRequest(@NotNull CourseStatus status) {
}
