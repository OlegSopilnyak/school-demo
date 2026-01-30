package oleg.sopilnyak.test.school.common.business.facade.profile.base;

import oleg.sopilnyak.test.school.common.business.facade.BusinessFacade;

/**
 * Service-BaseFacade: Service for manage person profiles in the school
 *
 */
public interface PersonProfileFacade extends BusinessFacade {
    /**
     * Action ID of find person by id
     *
     * @return action-id value
     */
    String findByIdActionId();

    /**
     * Action ID of create or update person by person instance
     *
     * @return created or updated person instance value
     */
    String createOrUpdateActionId();

    /**
     * Action ID of delete person by id
     *
     * @return action-id value
     */
    String deleteByIdActionId();

    /**
     * Unified facade's entry-point to do action and return the result
     *
     * @param actionId   the id of the action
     * @param parameters the parameters of the action to execute
     * @return action execution result value
     */
    @Override
    default <T> T doActionAndResult(String actionId, Object... parameters) {
        return personProfileAction(actionId, parameters);
    }

    /**
     * Facade depends on the action's execution (profiles related action)
     *
     * @param actionId   the id of the action
     * @param parameters the parameters of the action to execute
     * @param <T>        type of action execution result
     * @return action execution result value
     * @see BusinessFacade#doActionAndResult(String, Object...)
     */
    <T> T personProfileAction(String actionId, Object... parameters);
}
