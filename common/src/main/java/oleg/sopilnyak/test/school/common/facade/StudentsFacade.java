package oleg.sopilnyak.test.school.common.facade;

import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.exception.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.isNull;

/**
 * Service-Facade: Service for manage students in the school
 */
public interface StudentsFacade {
    /**
     * To get the student by ID
     *
     * @param id system-id of the student
     * @return student instance or empty() if not exists
     * @see Student
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Student> findById(Long id);

    /**
     * To get students enrolled to the course
     *
     * @param courseId system-id of the course
     * @return set of students
     */
    Set<Student> findEnrolledTo(Long courseId);

    /**
     * To get students enrolled to the course
     *
     * @param course course instance
     * @return set of students
     */
    default Set<Student> findEnrolledTo(Course course) {
        return isInvalid(course) ? Collections.emptySet() : findEnrolledTo(course.getId());
    }

    /**
     * To get students not enrolled to any course
     *
     * @return set of students
     */
    Set<Student> findNotEnrolled();

    /**
     * To create or update student instance
     *
     * @param student student should be created or updated
     * @return student instance or empty() if not exists
     * @see Student
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Student> createOrUpdate(Student student);

    /**
     * To delete student from the school
     *
     * @param studentId system-id of the student
     * @return true if success
     * @throws StudentNotExistsException   throws when student it not exists
     * @throws StudentWithCoursesException throws when student is not empty (has enrolled courses)
     */
    boolean delete(Long studentId) throws StudentNotExistsException, StudentWithCoursesException;

    /**
     * To delete student from the school
     *
     * @param student student instance
     * @return true if success
     * @throws StudentNotExistsException   throws when student it not exists
     * @throws StudentWithCoursesException throws when student is not empty (has enrolled courses)
     */
    default boolean delete(Student student) throws StudentNotExistsException, StudentWithCoursesException {
        return !isInvalid(student) && delete(student.getId());
    }

    private static boolean isInvalid(Student student) {
        return isNull(student) || isNull(student.getId());
    }

    private static boolean isInvalid(Course course) {
        return isNull(course) || isNull(course.getId());
    }

}
