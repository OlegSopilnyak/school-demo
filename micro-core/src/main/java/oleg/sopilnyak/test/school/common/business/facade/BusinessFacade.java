package oleg.sopilnyak.test.school.common.business.facade;

import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;

import java.util.List;

/**
 * Service-Facade: The parent of any Service Facades (has name of the facade)
 */
public interface BusinessFacade {

    /**
     * Unified facade's entry-point to do action and return the result
     *
     * @param actionId   the id of the action
     * @param parameters the parameters of the action to execute
     * @param <T>        type of action execution result
     * @return action execution result value
     */
    default <T> T doActionAndResult(String actionId, Object... parameters) {
        throw new UnsupportedOperationException("Please implement method in BusinessFacade's descendant.");
    }

    /**
     * To get the list of valid action-ids
     *
     * @return valid action-ids for concrete descendant-facade
     */
    default List<String> validActions() {
        throw new UnsupportedOperationException("Please implement method in BusinessFacade's descendant.");
    }

    /**
     * To throw invalid action-id exception
     *
     * @param actionId wrong action-id
     * @see InvalidParameterTypeException
     */
    default void throwInvalidActionId(final String actionId) {
        throw new InvalidParameterTypeException("Valid Action-ID of [" + getName() + "]", actionId);
    }

    /**
     * To get the name of the facade
     *
     * @return facade's name
     * @see ActionContext#getActionProcessorFacade()
     */
    String getName();
}
