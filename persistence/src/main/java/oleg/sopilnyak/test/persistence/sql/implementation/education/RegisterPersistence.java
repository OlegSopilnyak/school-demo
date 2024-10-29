package oleg.sopilnyak.test.persistence.sql.implementation.education;

import oleg.sopilnyak.test.persistence.sql.entity.education.CourseEntity;
import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.persistence.sql.repository.education.CourseRepository;
import oleg.sopilnyak.test.persistence.sql.repository.education.StudentRepository;
import oleg.sopilnyak.test.school.common.persistence.education.RegisterPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/**
 * Persistence facade implementation for student-course entities relations
 */
public interface RegisterPersistence extends RegisterPersistenceFacade {

    String STUDENT_NOT_FOUND = "Student '{}' does not exist";
    String COURSE_NOT_FOUND = "Course '{}' does not exist";

    Logger getLog();

    EntityMapper getMapper();

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
        return getStudentRepository().findStudentEntitiesByCourseSetId(id)
                .stream().map(Student.class::cast).collect(Collectors.toSet());
    }

    /**
     * To find not enrolled to any course students
     *
     * @return set of students
     */
    @Override
    default Set<Student> findNotEnrolledStudents() {
        getLog().debug("Looking for Not Enrolled Students");
        return getStudentRepository().findStudentEntitiesByCourseSetEmpty()
                .stream().map(Student.class::cast).collect(Collectors.toSet());
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
        return getCourseRepository().findCourseEntitiesByStudentSetId(id).stream()
                .map(Course.class::cast).collect(Collectors.toSet());
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
        final StudentEntity studentEntity = getStudentRepository().findById(student.getId()).orElse(null);
        if (isNull(studentEntity)) {
            getLog().warn(STUDENT_NOT_FOUND, student);
            return false;
        }

        final CourseEntity courseEntity = getCourseRepository().findById(course.getId()).orElse(null);
        if (isNull(courseEntity)) {
            getLog().warn(COURSE_NOT_FOUND, course);
            return false;
        }

        if (!studentEntity.add(courseEntity)) {
            getLog().warn("Course '{}' already exists", course);
            return false;
        }

        getStudentRepository().saveAndFlush(studentEntity);
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
        final StudentEntity studentEntity = getStudentRepository().findById(student.getId()).orElse(null);
        if (isNull(studentEntity)) {
            getLog().warn(STUDENT_NOT_FOUND, student);
            return false;
        }

        final CourseEntity courseEntity = getCourseRepository().findById(course.getId()).orElse(null);
        if (isNull(courseEntity)) {
            getLog().warn(COURSE_NOT_FOUND, course);
            return false;
        }

        if (!studentEntity.remove(courseEntity)) {
            getLog().warn("Course '{}' isn't exists", course);
            return false;
        }

        getStudentRepository().saveAndFlush(studentEntity);
        return true;
    }
}
