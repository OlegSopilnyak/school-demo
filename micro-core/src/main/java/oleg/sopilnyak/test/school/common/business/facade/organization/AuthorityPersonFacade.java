package oleg.sopilnyak.test.school.common.business.facade.organization;

import oleg.sopilnyak.test.school.common.business.facade.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;

import java.util.List;

/**
 * Service-Facade: Service for manage organization in the school (authority persons)
 *
 * @see OrganizationFacade
 * @see AuthorityPerson
 */
public interface AuthorityPersonFacade extends OrganizationFacade {
    //
    // action-ids
    String SUBSPACE = "::authority::person";
    String LOGIN = NAMESPACE + SUBSPACE + ":login";
    String LOGOUT = NAMESPACE + SUBSPACE + ":logout";
    String FIND_ALL = NAMESPACE + SUBSPACE + ":find.All";
    String FIND_BY_ID = NAMESPACE + SUBSPACE + ":find.By.Id";
    String CREATE_OR_UPDATE = NAMESPACE + SUBSPACE + ":create.Or.Update";
    String CREATE_MACRO = NAMESPACE + SUBSPACE + ":create.Macro";
    String DELETE = NAMESPACE + SUBSPACE + ":delete";
    String DELETE_MACRO = NAMESPACE + SUBSPACE + ":delete.Macro";
    //
    // the list of valid action-ids
    List<String> ACTION_IDS = List.of(
            LOGIN, LOGOUT, FIND_ALL, FIND_BY_ID, CREATE_OR_UPDATE, CREATE_MACRO, DELETE, DELETE_MACRO
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
        return "AuthorityPersonFacade";
    }
}
