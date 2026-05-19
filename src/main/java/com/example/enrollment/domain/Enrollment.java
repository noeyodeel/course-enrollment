package com.example.enrollment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EnrollmentStatus status = EnrollmentStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime confirmedAt;

    private LocalDateTime cancelledAt;

    protected Enrollment() {
    }

    public Enrollment(Course course, Long userId) {
        this.course = course;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public Long getUserId() {
        return userId;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void confirmPayment(LocalDateTime now) {
        if (status != EnrollmentStatus.PENDING) {
            throw new IllegalStateException("Only pending enrollment can be confirmed.");
        }
        status = EnrollmentStatus.CONFIRMED;
        confirmedAt = now;
    }

    public void cancel(LocalDateTime now) {
        if (status == EnrollmentStatus.CANCELLED) {
            throw new IllegalStateException("Already cancelled.");
        }
        status = EnrollmentStatus.CANCELLED;
        cancelledAt = now;
    }
}
