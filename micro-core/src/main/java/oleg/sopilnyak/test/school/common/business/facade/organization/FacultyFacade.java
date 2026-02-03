package oleg.sopilnyak.test.school.common.business.facade.organization;

import oleg.sopilnyak.test.school.common.business.facade.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.model.Faculty;

import java.util.List;

/**
 * Service-Facade: Service for manage organization in the school (groups of courses)
 *
 * @see OrganizationFacade
 * @see Faculty
 */
public interface FacultyFacade extends OrganizationFacade {
    String SUBSPACE = "::faculties";
    String FIND_ALL = NAMESPACE + SUBSPACE + ":find.All";
    String FIND_BY_ID = NAMESPACE + SUBSPACE + ":find.By.Id";
    String CREATE_OR_UPDATE = NAMESPACE + SUBSPACE + ":create.Or.Update";
    String DELETE = NAMESPACE + SUBSPACE + ":delete";
    //
    // the list of valid action-ids
    List<String> ACTION_IDS = List.of(
            FIND_ALL, FIND_BY_ID, CREATE_OR_UPDATE, DELETE
    );

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
}
