package oleg.sopilnyak.test.persistence.sql.implementation.students.courses;

import oleg.sopilnyak.test.persistence.sql.entity.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.SchoolEntityMapper;
import oleg.sopilnyak.test.persistence.sql.repository.CourseRepository;
import oleg.sopilnyak.test.persistence.sql.repository.StudentRepository;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.RegisterPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Persistence facade implementation for student-course entities relations
 */
public interface RegisterPersistenceFacadeImplementation extends RegisterPersistenceFacade {
    Logger getLog();

    SchoolEntityMapper getMapper();

    StudentRepository getStudentRepository();

    CourseRepository getCourseRepository();
    /**
     * To find enrolled students by course-id
     *
     * @param id system-id of the course
     * @return set of students
     */
    @Override
    default Set<Student> findEnrolledStudentsByCourseId(Long id) {
        getLog().debug("Looking for Enrolled Students by Course ID:{}", id);
        return getStudentRepository().findStudentEntitiesByCourseSetId(id).stream().map(Student.class::cast)
                .collect(Collectors.toSet());
    }

    /**
     * To find not enrolled to any course students
     *
     * @return set of students
     */
    @Override
    default Set<Student> findNotEnrolledStudents() {
        getLog().debug("Looking for Not Enrolled Students");
        return getStudentRepository().findStudentEntitiesByCourseSetEmpty().stream().map(Student.class::cast)
                .collect(Collectors.toSet());
    }

    /**
     * To find courses registered for student
     *
     * @param id system-id of student
     * @return set of courses
     */
    @Override
    default Set<Course> findCoursesRegisteredForStudent(Long id) {
        getLog().debug("Looking for Courses Registered to Student ID:{}", id);
        return getCourseRepository().findCourseEntitiesByStudentSetId(id).stream().map(Course.class::cast)
                .collect(Collectors.toSet());
    }

    /**
     * To find courses without students
     *
     * @return set of courses
     */
    @Override
    default Set<Course> findCoursesWithoutStudents() {
        getLog().debug("Looking for Courses Without Students");
        return getCourseRepository().findCourseEntitiesByStudentSetEmpty().stream().map(Course.class::cast)
                .collect(Collectors.toSet());
    }

    /**
     * To link the student with the course
     *
     * @param student student instance
     * @param course  course instance
     * @return true if linking successful
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    default boolean link(Student student, Course course) {
        getLog().debug("Linking the Student '{}'\n to the Course '{}'", student, course);
        final Optional<StudentEntity> studentEntity = getStudentRepository().findById(student.getId());
        if (studentEntity.isEmpty()) {
            return false;
        }

        final Optional<CourseEntity> courseEntity = getCourseRepository().findById(course.getId());
        if (courseEntity.isEmpty()) {
            return false;
        }

        if (!studentEntity.get().add(courseEntity.get())) {
            return false;
        }

        getStudentRepository().saveAndFlush(studentEntity.get());
        return true;
    }

    /**
     * To un-link the student from the course
     *
     * @param student student instance
     * @param course  course instance
     * @return true if un-linking successful
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    default boolean unLink(Student student, Course course) {
        getLog().debug("UnLinking the Student '{}'\n from the Course '{}'", student, course);
        final Optional<StudentEntity> studentEntity = getStudentRepository().findById(student.getId());
        if (studentEntity.isEmpty()) {
            return false;
        }

        final Optional<CourseEntity> courseEntity = getCourseRepository().findById(course.getId());
        if (courseEntity.isEmpty()) {
            return false;
        }

        if (!studentEntity.get().remove(courseEntity.get())) {
            return false;
        }

        getStudentRepository().saveAndFlush(studentEntity.get());
        return true;
    }
}
