package com.example.enrollment.service;

import com.example.enrollment.domain.Course;
import com.example.enrollment.domain.CourseStatus;
import com.example.enrollment.domain.EnrollmentStatus;
import com.example.enrollment.repository.CourseRepository;
import com.example.enrollment.repository.EnrollmentRepository;
import com.example.enrollment.web.dto.CourseCreateRequest;
import com.example.enrollment.web.dto.CourseResponse;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseService {

    private static final List<EnrollmentStatus> ACTIVE_ENROLLMENT_STATUSES =
            List.of(EnrollmentStatus.PENDING, EnrollmentStatus.CONFIRMED);

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public CourseService(CourseRepository courseRepository, EnrollmentRepository enrollmentRepository) {
        this.courseRepository = courseRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public CourseResponse create(Long creatorId, CourseCreateRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "endDate must be on or after startDate.");
        }
        Course course = new Course(
                creatorId,
                request.title(),
                request.description(),
                request.price(),
                request.capacity(),
                request.startDate(),
                request.endDate()
        );
        return toResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse changeStatus(Long courseId, Long creatorId, CourseStatus status) {
        Course course = getCourse(courseId);
        if (!course.getCreatorId().equals(creatorId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only creator can change course status.");
        }
        course.changeStatus(status);
        return toResponse(course);
    }

    @Transactional(readOnly = true)
    public List<CourseResponse> list(CourseStatus status) {
        Iterable<Course> courses = status == null ? courseRepository.findAll() : courseRepository.findByStatus(status);
        return ((List<Course>) courses).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CourseResponse detail(Long courseId) {
        return toResponse(getCourse(courseId));
    }

    private Course getCourse(Long courseId) {
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Course not found."));
    }

    private CourseResponse toResponse(Course course) {
        long enrolledCount = enrollmentRepository.countByCourseIdAndStatusIn(course.getId(), ACTIVE_ENROLLMENT_STATUSES);
        return CourseResponse.from(course, enrolledCount);
    }
}
