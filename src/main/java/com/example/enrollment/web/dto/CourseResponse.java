package com.example.enrollment.web.dto;

import com.example.enrollment.domain.Course;
import com.example.enrollment.domain.CourseStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record CourseResponse(
        Long id,
        Long creatorId,
        String title,
        String description,
        BigDecimal price,
        int capacity,
        long enrolledCount,
        LocalDate startDate,
        LocalDate endDate,
        CourseStatus status,
        LocalDateTime createdAt
) {

    public static CourseResponse from(Course course, long enrolledCount) {
        return new CourseResponse(
                course.getId(),
                course.getCreatorId(),
                course.getTitle(),
                course.getDescription(),
                course.getPrice(),
                course.getCapacity(),
                enrolledCount,
                course.getStartDate(),
                course.getEndDate(),
                course.getStatus(),
                course.getCreatedAt()
        );
    }
}
