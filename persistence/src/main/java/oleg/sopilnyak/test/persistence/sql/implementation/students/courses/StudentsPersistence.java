package oleg.sopilnyak.test.persistence.sql.implementation.students.courses;

import oleg.sopilnyak.test.persistence.sql.entity.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.SchoolEntityMapper;
import oleg.sopilnyak.test.persistence.sql.repository.StudentRepository;
import oleg.sopilnyak.test.school.common.persistence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Persistence facade implementation for students entities
 */
public interface StudentsPersistence extends StudentsPersistenceFacade {
    Logger getLog();

    SchoolEntityMapper getMapper();

    StudentRepository getStudentRepository();

    /**
     * To find student by id
     *
     * @param id system-id of the student
     * @return student instance or empty() if not exists
     * @see Student
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    default Optional<Student> findStudentById(Long id) {
        getLog().debug("Looking for Student with ID:{}", id);
        return getStudentRepository().findById(id).map(Student.class::cast);
    }

    /**
     * Create or update student
     *
     * @param student student instance to store
     * @return student instance or empty(), if instance couldn't store
     * @see Student
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    default Optional<Student> save(Student student) {
        getLog().debug("Create or Update {}", student);
        final StudentEntity entity = student instanceof StudentEntity s ? s : getMapper().toEntity(student);
        return Optional.of(getStudentRepository().saveAndFlush(entity));
    }

    /**
     * Delete student by id
     *
     * @param id system-id of the student
     * @return true if student deletion successfully
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    default boolean deleteStudent(Long id) {
        getLog().debug("Deleting Student with ID:{}", id);
        getStudentRepository().deleteById(id);
        getStudentRepository().flush();
        return true;
    }

    /**
     * Convert student to entity bean
     *
     * @param student instance to convert
     * @return instance ready to use in the repository
     */
    @Override
    default Student toEntity(Student student) {
        return getMapper().toEntity(student);
    }
}
