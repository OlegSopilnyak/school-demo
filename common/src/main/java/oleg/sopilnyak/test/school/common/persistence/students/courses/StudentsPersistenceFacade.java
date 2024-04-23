package oleg.sopilnyak.test.school.common.persistence.students.courses;

import oleg.sopilnyak.test.school.common.model.Student;

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
     * Convert student to entity bean
     *
     * @param student instance to convert
     * @return instance ready to use in the repository
     */
    Student toEntity(Student student);
}
