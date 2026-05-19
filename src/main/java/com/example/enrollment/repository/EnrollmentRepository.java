package com.example.enrollment.repository;

import com.example.enrollment.domain.Enrollment;
import com.example.enrollment.domain.EnrollmentStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    long countByCourseIdAndStatusIn(Long courseId, Collection<EnrollmentStatus> statuses);

    boolean existsByCourseIdAndUserIdAndStatusIn(Long courseId, Long userId, Collection<EnrollmentStatus> statuses);

    Optional<Enrollment> findByIdAndUserId(Long id, Long userId);

    Page<Enrollment> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Enrollment> findByCourseIdAndStatusInOrderByCreatedAtAsc(Long courseId, Collection<EnrollmentStatus> statuses);
}
