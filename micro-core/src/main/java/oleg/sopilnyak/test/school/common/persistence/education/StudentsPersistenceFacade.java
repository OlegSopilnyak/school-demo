package oleg.sopilnyak.test.school.common.persistence.education;

import oleg.sopilnyak.test.school.common.model.education.Student;

import java.util.Optional;

/**
 * Persistence facade for students entities
 */
public interface StudentsPersistenceFacade {
    /**
     * To find student by id
     *
     * @param id system-id of the student
     * @return student instance or empty() if not exists
     * @see Student
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Student> findStudentById(Long id);

    /**
     * Create or update student
     *
     * @param student student instance to store
     * @return student instance or empty(), if instance couldn't store
     * @see Student
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
     * To check is there is any student in the database<BR/>For tests purposes only
     *
     * @return true if there is no student in database
     */
    boolean isNoStudents();
}
