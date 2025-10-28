package oleg.sopilnyak.test.persistence.sql.implementation.education;

import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.persistence.sql.repository.education.CourseRepository;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.education.CoursesPersistenceFacade;

import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistence facade implementation for courses entities
 */
public interface CoursesPersistence extends CoursesPersistenceFacade {
    Logger getLog();

    EntityMapper getMapper();

    CourseRepository getCourseRepository();

    /**
     * To find course by id
     *
     * @param id system-id of the course
     * @return student instance or empty() if not exists
     * @see Course
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    default Optional<Course> findCourseById(Long id) {
        getLog().debug("Looking for Course with ID:{}", id);
        return getCourseRepository().findById(id).map(Course.class::cast);
    }

    /**
     * Create or update course
     *
     * @param course course instance to store
     * @return course instance or empty(), if instance couldn't store
     * @see Course
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    default Optional<Course> save(Course course) {
        getLog().debug("Create or Update {}", course);
        final CourseEntity entity = course instanceof CourseEntity c ? c : getMapper().toEntity(course);
        return Optional.of(getCourseRepository().saveAndFlush(entity));
    }

    /**
     * Delete course by id
     *
     * @param id system-id of the course
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    default void deleteCourse(Long id) {
        getLog().debug("Deleting Course with ID:{}", id);
        getCourseRepository().deleteById(id);
        getCourseRepository().flush();
    }

    /**
     * To check is there is any course in the database<BR/>For tests purposes only
     *
     * @return true if there is no course in database
     */
    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    default boolean isNoCourses() {
        return getCourseRepository().count() == 0;
    }
}
