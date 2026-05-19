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
@Table(name = "enrollment_queues")
public class EnrollmentQueue {

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
    private EnrollmentQueueStatus status = EnrollmentQueueStatus.WAITING;

    private LocalDateTime readyAt;

    private LocalDateTime expiresAt;

    private LocalDateTime completedAt;

    private LocalDateTime cancelledAt;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected EnrollmentQueue() {
    }

    public EnrollmentQueue(Course course, Long userId) {
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

    public EnrollmentQueueStatus getStatus() {
        return status;
    }

    public LocalDateTime getReadyAt() {
        return readyAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getCancelledAt() {
        return cancelledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isExpired(LocalDateTime now) {
        return status == EnrollmentQueueStatus.READY && expiresAt != null && expiresAt.isBefore(now);
    }

    public void markReady(LocalDateTime now, LocalDateTime expiresAt) {
        status = EnrollmentQueueStatus.READY;
        readyAt = now;
        this.expiresAt = expiresAt;
    }

    public void complete(LocalDateTime now) {
        if (status != EnrollmentQueueStatus.READY) {
            throw new IllegalStateException("Only ready queue can be completed.");
        }
        status = EnrollmentQueueStatus.COMPLETED;
        completedAt = now;
    }

    public void expire() {
        if (status == EnrollmentQueueStatus.READY) {
            status = EnrollmentQueueStatus.EXPIRED;
        }
    }

    public void cancel(LocalDateTime now) {
        if (status == EnrollmentQueueStatus.COMPLETED) {
            throw new IllegalStateException("Completed queue cannot be cancelled.");
        }
        status = EnrollmentQueueStatus.CANCELLED;
        cancelledAt = now;
    }
}
