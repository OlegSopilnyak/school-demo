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
    String PREFIX = "organization.authority.person";
    String LOGIN = PREFIX + ".login";
    String LOGOUT = PREFIX + ".logout";
    String FIND_ALL = PREFIX + ".findAll";
    String FIND_BY_ID = PREFIX + ".findById";
    String CREATE_OR_UPDATE = PREFIX + ".createOrUpdate";
    String CREATE_MACRO = PREFIX + ".create.Macro";
    String DELETE = PREFIX + ".delete";
    String DELETE_MACRO = PREFIX + ".delete.Macro";
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
