package oleg.sopilnyak.test.school.common.persistence.organization;

import oleg.sopilnyak.test.school.common.exception.organization.StudentsGroupNotFoundException;
import oleg.sopilnyak.test.school.common.exception.organization.StudentGroupWithStudentsException;
import oleg.sopilnyak.test.school.common.model.organization.StudentsGroup;

import java.util.Optional;
import java.util.Set;

/**
 * Persistence facade for organization structure entities (school students groups)
 *
 * @see StudentsGroup
 */
public interface StudentsGroupPersistenceFacade {
    /**
     * To get all students groups of the school
     *
     * @return the set of students groups
     * @see StudentsGroup
     */
    Set<StudentsGroup> findAllStudentsGroups();

    /**
     * To find students group by id
     *
     * @param id system-id of the students group
     * @return students group instance or empty() if not exists
     * @see StudentsGroup
     * @see Optional
     * @see Optional#empty()
     */
    Optional<StudentsGroup> findStudentsGroupById(Long id);

    /**
     * Create or update students group instance
     *
     * @param instance students group instance to store
     * @return students group instance or empty(), if instance couldn't store
     * @see StudentsGroup
     * @see Optional
     * @see Optional#empty()
     */
    Optional<StudentsGroup> save(StudentsGroup instance);

    /**
     * To delete students group by id
     *
     * @param id system-id of the students group
     * @throws StudentsGroupNotFoundException    throws when you want to delete students group which is not created before
     * @throws StudentGroupWithStudentsException throws when you want to delete students group with students
     * @see StudentsGroup
     */
    void deleteStudentsGroup(Long id) throws
            StudentsGroupNotFoundException,
            StudentGroupWithStudentsException;
}
