package oleg.sopilnyak.test.persistence.sql.implementation.education;

import oleg.sopilnyak.test.persistence.sql.entity.education.StudentEntity;
import oleg.sopilnyak.test.persistence.sql.mapper.EntityMapper;
import oleg.sopilnyak.test.persistence.sql.repository.education.StudentRepository;
import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.school.common.persistence.education.StudentsPersistenceFacade;

import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persistence facade implementation for students entities
 */
public interface StudentsPersistence extends StudentsPersistenceFacade {
    Logger getLog();

    EntityMapper getMapper();

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
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
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
     * To check is there are any student in the database<BR/>For tests purposes only
     *
     * @return true if there is no student in database
     */
    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    default boolean isNoStudents() {
        return getStudentRepository().count() == 0L;
    }
}
