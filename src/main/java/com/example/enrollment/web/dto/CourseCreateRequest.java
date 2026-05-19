package com.example.enrollment.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CourseCreateRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 2000) String description,
        @NotNull @DecimalMin(value = "0.0") BigDecimal price,
        @Min(1) int capacity,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {
}
