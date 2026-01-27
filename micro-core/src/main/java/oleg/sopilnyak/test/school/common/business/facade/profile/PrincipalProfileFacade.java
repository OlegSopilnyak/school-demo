package oleg.sopilnyak.test.school.common.business.facade.profile;

import oleg.sopilnyak.test.school.common.business.facade.BusinessFacade;
import oleg.sopilnyak.test.school.common.business.facade.profile.base.PersonProfileFacade;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;

import java.util.List;
import java.util.Optional;

/**
 * Service-Facade: Service for manage principal profiles in the school
 *
 * @see PrincipalProfile
 * @see PersonProfileFacade
 */
public interface PrincipalProfileFacade extends PersonProfileFacade, BusinessFacade {
    //
    // action-ids
    String PREFIX = "profile.principal";
    String FIND_BY_ID = PREFIX + ".findById";
    String CREATE_OR_UPDATE = PREFIX + ".createOrUpdate";
    String DELETE_BY_ID = PREFIX + ".deleteById";
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
     * To do action and return the result
     *
     * @param actionId         the id of the action
     * @param actionParameters the parameters of action to execute
     * @param <T>              type of action execution result
     * @return action execution result value
     */
    @Override
    default <T> T doActionAndResult(String actionId, Object... actionParameters) {
        final String validActionId = ACTION_IDS.stream().filter(id -> id.equals(actionId)).findFirst()
                .orElseThrow(
                        () -> new InvalidParameterTypeException(String.join(" or ", ACTION_IDS), actionId)
                );
        return concreteAction(validActionId, actionParameters);
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

    /**
     * To get the principal's profile by ID
     *
     * @param id system-id of the principal profile
     * @return profile instance or empty() if not exists
     * @see PrincipalProfile
     * @see PrincipalProfile#getId()
     * @see Optional
     * @see Optional#empty()
     * @deprecated
     */
    @Deprecated
    default Optional<PrincipalProfile> findPrincipalProfileById(Long id) {
        return findById(id).map(p -> p instanceof PrincipalProfile profile ? profile : null);
    }

    /**
     * To create principal-profile
     *
     * @param input instance to create
     * @return created instance or Optional#empty()
     * @see PrincipalProfile
     * @see Optional
     * @see Optional#empty()
     * @deprecated
     */
    @Deprecated
    default Optional<PrincipalProfile> createOrUpdateProfile(PrincipalProfile input) {
        return createOrUpdate(input).map(p -> p instanceof PrincipalProfile profile ? profile : null);
    }
}
