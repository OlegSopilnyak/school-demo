package oleg.sopilnyak.test.school.common.persistence.organization;

import oleg.sopilnyak.test.school.common.exception.organization.FacultyIsNotEmptyException;
import oleg.sopilnyak.test.school.common.exception.organization.FacultyNotFoundException;
import oleg.sopilnyak.test.school.common.model.organization.Faculty;

import java.util.Optional;
import java.util.Set;

/**
 * Persistence facade for organization structure entities (groups of courses)
 *
 * @see Faculty
 */
public interface FacultyPersistenceFacade {
    /**
     * To get all faculties of the school
     *
     * @return the set of faculties
     * @see Faculty
     */
    Set<Faculty> findAllFaculties();

    /**
     * To find faculty by id
     *
     * @param id system-id of the faculty
     * @return faculty instance or empty() if not exists
     * @see Faculty
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Faculty> findFacultyById(Long id);

    /**
     * Create or update faculty instance
     *
     * @param instance faculty instance to store
     * @return faculty instance or empty(), if instance couldn't store
     * @see Faculty
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Faculty> save(Faculty instance);

    /**
     * To delete faculty by id
     *
     * @param id system-id of the faculty
     * @throws FacultyNotFoundException  throws when you want to delete faculty which is not created before
     * @throws FacultyIsNotEmptyException throws when you want to delete faculty which has courses
     * @see Faculty
     */
    void deleteFaculty(Long id) throws
            FacultyNotFoundException,
            FacultyIsNotEmptyException;
}
