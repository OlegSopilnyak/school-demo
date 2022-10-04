package oleg.sopilnyak.test.school.common.facade;

import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;

import java.util.Optional;
import java.util.Set;

/**
 * Service-Facade: Service for manage persistence layer of the school
 */
public interface PersistenceFacade {
    /**
     * To initialize default minimal data-set for the application
     */
    void initDefaultDataset();

    /**
     * To find student by id
     *
     * @param id system-id of the student
     * @return student instance or empty() if not exists
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Student> findStudentById(Long id);

    /**
     * To find enrolled students by course-id
     *
     * @param courseId system-id of the course
     * @return set of students
     */
    Set<Student> findEnrolledStudentsByCourseId(Long courseId);

    /**
     * To find not enrolled to any course students
     *
     * @return set of students
     */
    Set<Student> findNotEnrolledStudents();

    /**
     * Create or update student
     *
     * @param student student instance to store
     * @return student instance or empty(), if instance couldn't store
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Student> save(Student student);

    /**
     * Delete student by id
     *
     * @param studentId system-id of the student
     * @return true if student deletion successfully
     */
    boolean deleteStudent(Long studentId);

    /**
     * To find course by id
     *
     * @param id system-id of the course
     * @return student instance or empty() if not exists
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Course> findCourseById(Long id);

    /**
     * To find courses registered for student
     *
     * @param studentId system-id of student
     * @return set of courses
     */
    Set<Course> findCoursesRegisteredForStudent(Long studentId);

    /**
     * To find courses without students
     *
     * @return set of courses
     */
    Set<Course> findCoursesWithoutStudents();

    /**
     * Create or update course
     *
     * @param course course instance to store
     * @return course instance or empty(), if instance couldn't store
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

    /**
     * To link the student with the course
     *
     * @param student student instance
     * @param course  course instance
     * @return true if linking successful
     */
    boolean link(Student student, Course course);

    /**
     * To un-link the student from the course
     *
     * @param student student instance
     * @param course  course instance
     * @return true if un-linking successful
     */
    boolean unLink(Student student, Course course);
}
