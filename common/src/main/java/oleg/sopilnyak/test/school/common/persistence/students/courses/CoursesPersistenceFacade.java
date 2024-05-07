package oleg.sopilnyak.test.school.common.persistence.students.courses;

import oleg.sopilnyak.test.school.common.model.Course;

import java.util.Optional;

/**
 * Persistence facade for courses entities
 *
 * @see Course
 */
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
     */
    void deleteCourse(Long courseId);

    /**
     * Convert course to entity bean
     *
     * @param course instance to convert
     * @return instance ready to use in the repository
     */
    Course toEntity(Course course);

    /**
     * To check is there is any course in the database<BR/>For tests purposes only
     *
     * @return true if there is no course in database
     */
    boolean isNoCourses();
}
