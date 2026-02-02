package oleg.sopilnyak.test.school.common.business.facade.organization;

import oleg.sopilnyak.test.school.common.business.facade.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.exception.organization.StudentGroupWithStudentsException;
import oleg.sopilnyak.test.school.common.exception.organization.StudentsGroupNotFoundException;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service-Facade: Service for manage organization in the school (school students groups)
 *
 * @see OrganizationFacade
 * @see StudentsGroup
 */
public interface StudentsGroupFacade extends OrganizationFacade {
    String SUBSPACE = "::students::group";
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
        return "StudentsGroupFacade";
    }

    /**
     * To get all faculties
     *
     * @return list of faculties
     * @see StudentsGroup
     * @deprecated
     */
    @Deprecated
    Collection<StudentsGroup> findAllStudentsGroups();

    /**
     * To get the students group by ID
     *
     * @param id system-id of the students group
     * @return StudentsGroup instance or empty() if not exists
     * @see StudentsGroup
     * @see Optional
     * @see Optional#empty()
     * @deprecated
     */
    @Deprecated
    Optional<StudentsGroup> findStudentsGroupById(Long id);

    /**
     * To create or update students group instance
     *
     * @param instance students group should be created or updated
     * @return students group instance or empty() if not exists
     * @see StudentsGroup
     * @see Optional
     * @see Optional#empty()
     * @deprecated
     */
    @Deprecated
    Optional<StudentsGroup> createOrUpdateStudentsGroup(StudentsGroup instance);

    /**
     * To delete students group instance from the school
     *
     * @param id system-id of the faculty to delete
     * @throws StudentsGroupNotFoundException   throws when students group is not exists
     * @throws StudentGroupWithStudentsException throws when students group has students
     * @deprecated
     */
    @Deprecated
    void deleteStudentsGroupById(Long id)
            throws StudentsGroupNotFoundException,
            StudentGroupWithStudentsException;

    default void deleteStudentsGroup(StudentsGroup instance)
            throws StudentsGroupNotFoundException,
            StudentGroupWithStudentsException {
        if (isInvalid(instance)) {
            throw new StudentsGroupNotFoundException("Wrong " + instance + " to delete");
        }
        deleteStudentsGroupById(instance.getId());
    }

}
