package oleg.sopilnyak.test.school.common.business.facade.organization;

import oleg.sopilnyak.test.school.common.business.facade.BusinessFacade;
import oleg.sopilnyak.test.school.common.business.facade.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;

import java.util.List;

/**
 * Service-Facade: Service for manage organization in the school (authority persons)
 *
 * @see OrganizationFacade
 * @see AuthorityPerson
 */
public interface AuthorityPersonFacade extends OrganizationFacade, BusinessFacade {
    //
    // action-ids
    String LOGIN = NAMESPACE + ":login";
    String LOGOUT = NAMESPACE + ":logout";
    String FIND_ALL = NAMESPACE + ":find.All";
    String FIND_BY_ID = NAMESPACE + ":find.By.Id";
    String CREATE_OR_UPDATE = NAMESPACE + ":create.Or.Update";
    String CREATE_MACRO = NAMESPACE + ":create.Macro";
    String DELETE = NAMESPACE + ":delete";
    String DELETE_MACRO = NAMESPACE + ":delete.Macro";
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
