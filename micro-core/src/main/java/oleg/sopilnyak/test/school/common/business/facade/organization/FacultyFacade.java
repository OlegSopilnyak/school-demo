package oleg.sopilnyak.test.school.common.business.facade.organization;

import oleg.sopilnyak.test.school.common.business.facade.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.exception.organization.FacultyIsNotEmptyException;
import oleg.sopilnyak.test.school.common.exception.organization.FacultyNotFoundException;
import oleg.sopilnyak.test.school.common.model.Faculty;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service-Facade: Service for manage organization in the school (groups of courses)
 *
 * @see OrganizationFacade
 * @see Faculty
 */
public interface FacultyFacade extends OrganizationFacade {
    String SUBSPACE = "::faculty";
    String FIND_ALL = NAMESPACE + SUBSPACE + ":find.All";
    String FIND_BY_ID = NAMESPACE + SUBSPACE + ":find.By.Id";
    String CREATE_OR_UPDATE = NAMESPACE + SUBSPACE + ":create.Or.Update";
    String DELETE = NAMESPACE + SUBSPACE + ":delete";
    //
    // the list of valid action-ids
    List<String> ACTION_IDS = List.of(FIND_ALL, FIND_BY_ID, CREATE_OR_UPDATE, DELETE);

    /**
     * To get the list of valid action-ids
     *
     * @return valid action-ids for concrete descendant-facade
     */
    @Override
    default List<String> validActions() {
        return ACTION_IDS;
    }

    /**
     * To get the name of the facade
     *
     * @return facade's name
     */
    @Override
    default String getName() {
        return "FacultyFacade";
    }

    /**
     * To get all faculties
     *
     * @see Faculty
     * @return list of faculties
     * @deprecated
     */
    @Deprecated
    Collection<Faculty> findAllFaculties();

    /**
     * To get the faculty by ID
     *
     * @param id system-id of the faculty
     * @return Faculty instance or empty() if not exists
     * @see Faculty
     * @see Optional
     * @see Optional#empty()
     * @deprecated
     */
    @Deprecated
    Optional<Faculty> findFacultyById(Long id);

    /**
     * To create or update faculty instance
     *
     * @param instance faculty should be created or updated
     * @return faculty instance or empty() if not exists
     * @see Faculty
     * @see Optional
     * @see Optional#empty()
     * @deprecated
     */
    @Deprecated
    Optional<Faculty> createOrUpdateFaculty(Faculty instance);

    /**
     * To delete faculty from the school
     *
     * @param id system-id of the faculty to delete
     * @throws FacultyNotFoundException  throws when faculty is not exists
     * @throws FacultyIsNotEmptyException throws when faculty has courses
     * @deprecated
     */
    @Deprecated
    void deleteFacultyById(Long id)
            throws FacultyNotFoundException,
            FacultyIsNotEmptyException;

    default void deleteFaculty(Faculty instance)
            throws FacultyNotFoundException,
            FacultyIsNotEmptyException {
        if (isInvalid(instance)) {
            throw new FacultyNotFoundException("Wrong " + instance + " to delete");
        }
        deleteFacultyById(instance.getId());
    }

}
