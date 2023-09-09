package oleg.sopilnyak.test.school.common.facade.peristence;

import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;

import java.util.Set;

public interface RegisterPersistenceFacade {

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
