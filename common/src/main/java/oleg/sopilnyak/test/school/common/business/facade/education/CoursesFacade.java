package oleg.sopilnyak.test.school.common.business.facade.education;

import oleg.sopilnyak.test.school.common.business.facade.BusinessFacade;
import oleg.sopilnyak.test.school.common.exception.education.*;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.BaseType;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.isNull;

/**
 * Service-Facade: Service for manage courses in the school
 *
 * @see Student
 * @see Course
 */
public interface CoursesFacade extends BusinessFacade {
    /**
     * To get the name of the facade
     *
     * @return facade's name
     */
    @Override
    default String getName() {
        return "CoursesFacade";
    }

    /**
     * To get the course by ID
     *
     * @param id system-id of the course
     * @return student instance or empty() if not exists
     * @see Course
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Course> findById(Long id);

    /**
     * To get courses registered for the student
     *
     * @param studentId system-id of the student
     * @return set of courses
     */
    Set<Course> findRegisteredFor(Long studentId);

    /**
     * To get courses registered for the student
     *
     * @param student student instance
     * @return set of courses
     */
    default Set<Course> findRegisteredFor(Student student) {
        return isInvalid(student) ? Collections.emptySet() : findRegisteredFor(student.getId());
    }

    /**
     * To get courses without registered students
     *
     * @return set of courses
     */
    Set<Course> findWithoutStudents();

    /**
     * To create or update course instance
     *
     * @param course course should be created or updated
     * @return student instance or empty() if not exists
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Course> createOrUpdate(Course course);

    /**
     * To delete course from the school
     *
     * @param courseId system-id of the course to delete
     * @throws CourseNotFoundException     throws when course it not exists
     * @throws CourseWithStudentsException throws when course is not empty (has registered students)
     */
    void delete(Long courseId) throws CourseNotFoundException, CourseWithStudentsException;

    /**
     * To delete course from the school
     *
     * @param course course instance to delete
     * @throws CourseNotFoundException     throws when course it not exists
     * @throws CourseWithStudentsException throws when course is not empty (has registered students)
     */
    default void delete(Course course) throws CourseNotFoundException, CourseWithStudentsException {
        if (isInvalid(course)) {
            throw new CourseNotFoundException("Wrong " + course + " to delete.");
        }
        delete(course.getId());
    }

    /**
     * To register the student to the school course
     *
     * @param studentId system-id of the student
     * @param courseId  system-id of the course
     * @throws StudentNotFoundException      throws when student is not exists
     * @throws CourseNotFoundException       throws if course is not exists
     * @throws CourseHasNoRoomException    throws when there is no free slots for student
     * @throws StudentCoursesExceedException throws when student already registered to a lot ot courses
     */
    void register(Long studentId, Long courseId) throws
            StudentNotFoundException, CourseNotFoundException,
            CourseHasNoRoomException, StudentCoursesExceedException;

    /**
     * To register the student to the school course
     *
     * @param student student instance
     * @param course  course instance
     * @throws StudentNotFoundException      throws when student is not exists
     * @throws CourseNotFoundException       throws if course is not exists
     * @throws CourseHasNoRoomException    throws when there is no free slots for student
     * @throws StudentCoursesExceedException throws when student already registered to a lot ot courses
     */
    default void register(Student student, Course course) throws
            StudentNotFoundException, CourseNotFoundException,
            CourseHasNoRoomException, StudentCoursesExceedException {
        if (isInvalid(student)) {
            throw new StudentNotFoundException("Wrong student " + student + " for registration.");
        } else if (isInvalid(course)) {
            throw new CourseNotFoundException("Wrong course " + course + " for registration.");
        }
        register(student.getId(), course.getId());
    }

    /**
     * To un-register the student from the school course
     *
     * @param studentId system-id of the student
     * @param courseId  system-id of the course
     * @throws StudentNotFoundException throws when student is not exists
     * @throws CourseNotFoundException  throws if course is not exists
     */
    void unRegister(Long studentId, Long courseId) throws StudentNotFoundException, CourseNotFoundException;

    /**
     * To un-register the student from the school course
     *
     * @param student student instance
     * @param course  course instance
     * @throws StudentNotFoundException throws when student is not exists
     * @throws CourseNotFoundException  throws if course is not exists
     */
    default void unRegister(Student student, Course course) throws StudentNotFoundException, CourseNotFoundException {
        if (isInvalid(student)) {
            throw new StudentNotFoundException("Wrong student " + student + " for un-registration.");
        } else if (isInvalid(course)) {
            throw new CourseNotFoundException("Wrong course " + course + " for un-registration.");
        }
        unRegister(student.getId(), course.getId());
    }

    private static boolean isInvalid(BaseType item) {
        return isNull(item) || isNull(item.getId());
    }
}

