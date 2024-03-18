package oleg.sopilnyak.test.persistence.sql.implementation.students.courses;

import oleg.sopilnyak.test.persistence.sql.entity.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.SchoolEntityMapper;
import oleg.sopilnyak.test.persistence.sql.repository.CourseRepository;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Persistence facade implementation for courses entities
 */
public interface CoursesPersistenceFacadeImplementation extends CoursesPersistenceFacade {
    Logger getLog();

    SchoolEntityMapper getMapper();

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
     * @return true if the course deletion successfully
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    default boolean deleteCourse(Long id) {
        getLog().debug("Deleting Course with ID:{}", id);
        getCourseRepository().deleteById(id);
        getCourseRepository().flush();
        return true;
    }
}
