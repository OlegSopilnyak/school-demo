package oleg.sopilnyak.test.school.common.business.organization;

import oleg.sopilnyak.test.school.common.exception.FacultyIsNotEmptyException;
import oleg.sopilnyak.test.school.common.exception.NotExistFacultyException;
import oleg.sopilnyak.test.school.common.business.OrganizationFacade;
import oleg.sopilnyak.test.school.common.model.Faculty;

import java.util.Collection;
import java.util.Optional;

/**
 * Service-Facade: Service for manage organization in the school (groups of courses)
 *
 * @see OrganizationFacade
 * @see Faculty
 */
public interface FacultyFacade extends OrganizationFacade {

    /**
     * To get all faculties
     *
     * @see Faculty
     * @return list of faculties
     */
    Collection<Faculty> findAllFaculties();

    /**
     * To get the faculty by ID
     *
     * @param id system-id of the faculty
     * @return Faculty instance or empty() if not exists
     * @see Faculty
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Faculty> getFacultyById(Long id);

    /**
     * To create or update faculty instance
     *
     * @param instance faculty should be created or updated
     * @return faculty instance or empty() if not exists
     * @see Faculty
     * @see Optional
     * @see Optional#empty()
     */
    Optional<Faculty> createOrUpdateFaculty(Faculty instance);

    /**
     * To delete faculty from the school
     *
     * @param id system-id of the faculty to delete
     * @throws NotExistFacultyException  throws when faculty is not exists
     * @throws FacultyIsNotEmptyException throws when faculty has courses
     */
    void deleteFacultyById(Long id)
            throws NotExistFacultyException,
            FacultyIsNotEmptyException;

    default void deleteFaculty(Faculty instance)
            throws NotExistFacultyException,
            FacultyIsNotEmptyException {
        if (isInvalid(instance)) {
            throw new NotExistFacultyException("Wrong " + instance + " to delete");
        }
        deleteFacultyById(instance.getId());
    }

}
