package oleg.sopilnyak.test.school.common.facade.peristence;

import oleg.sopilnyak.test.school.common.model.Course;

import java.util.Optional;

public interface CoursesPersistenceFacade {
    /**
     * To find course by id
     *
     * @param id system-id of the course
     * @return student instance or empty() if not exists
     * @see Course
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Course> findCourseById(Long id);

    /**
     * Create or update course
     *
     * @param course course instance to store
     * @return course instance or empty(), if instance couldn't store
     * @see Course
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Course> save(Course course);

    /**
     * Delete course by id
     *
     * @param courseId system-id of the course
     * @return true if the course deletion successfully
     */
    boolean deleteCourse(Long courseId);
}
