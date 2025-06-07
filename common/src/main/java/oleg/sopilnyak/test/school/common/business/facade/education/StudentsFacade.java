package oleg.sopilnyak.test.school.common.business.facade.education;

import oleg.sopilnyak.test.school.common.business.facade.BusinessFacade;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.BaseType;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.isNull;

/**
 * Service-Facade: Service for manage students in the school
 */
public interface StudentsFacade extends BusinessFacade {
    /**
     * To get the name of the facade
     *
     * @return facade's name
     */
    @Override
    default String getName() {
        return "StudentsFacade";
    }

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
     * To create student instance + it's profile
     *
     * @param student student should be created
     * @return student instance or empty() if not exists
     * @see Student
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Student> create(Student student);

    /**
     * To delete student from the school
     *
     * @param studentId system-id of the student
     * @return true if success
     * @throws StudentNotFoundException    throws when student it not exists
     * @throws StudentWithCoursesException throws when student is not empty (has enrolled courses)
     */
    boolean delete(Long studentId) throws StudentNotFoundException, StudentWithCoursesException;

    /**
     * To delete student from the school
     *
     * @param student student instance
     * @return true if success
     * @throws StudentNotFoundException    throws when student it not exists
     * @throws StudentWithCoursesException throws when student is not empty (has enrolled courses)
     */
    default boolean delete(Student student) throws StudentNotFoundException, StudentWithCoursesException {
        return !isInvalid(student) && delete(student.getId());
    }

    private static boolean isInvalid(BaseType item) {
        return isNull(item) || isNull(item.getId());
    }
}
