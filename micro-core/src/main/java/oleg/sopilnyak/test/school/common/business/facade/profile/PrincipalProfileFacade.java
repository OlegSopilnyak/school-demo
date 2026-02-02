package oleg.sopilnyak.test.school.common.business.facade.profile;

import oleg.sopilnyak.test.school.common.business.facade.profile.base.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;

import java.util.List;

/**
 * Service-Facade: Service for manage principal profiles in the school
 *
 * @see PrincipalProfile
 * @see PersonProfileFacade
 */
public interface PrincipalProfileFacade extends PersonProfileFacade {
    //
    // action-ids
    String SUBSPACE = "::principal";
    String FIND_BY_ID = NAMESPACE + SUBSPACE + ":find.By.Id";
    String CREATE_OR_UPDATE = NAMESPACE+ SUBSPACE  + ":create.Or.Update";
    String DELETE_BY_ID = NAMESPACE+ SUBSPACE  + ":delete.By.Id";
    //
    // the list of valid action-ids
    List<String> ACTION_IDS = List.of(FIND_BY_ID, CREATE_OR_UPDATE, DELETE_BY_ID);

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
     * Action ID of find person by id
     *
     * @return action-id value
     */
    @Override
    default String findByIdActionId() {
        return FIND_BY_ID;
    }

    /**
     * Action ID of create or update person by person instance
     *
     * @return created or updated person instance value
     */
    @Override
    default String createOrUpdateActionId() {
        return CREATE_OR_UPDATE;
    }

    /**
     * Action ID of delete person by id
     *
     * @return action-id value
     */
    @Override
    default String deleteByIdActionId() {
        return DELETE_BY_ID;
    }

    /**
     * To get the name of the facade
     *
     * @return facade's name
     */
    @Override
    default String getName() {
        return "PrincipalProfileFacade";
    }
}
