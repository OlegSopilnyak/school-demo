package oleg.sopilnyak.test.school.common.business.facade;

import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.model.BaseType;

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

    /**
     * To check the state of model's item
     *
     * @param item instance to check
     * @return true if instance is invalid (empty or has invalid system-id)
     */
    default boolean isInvalid(final BaseType item) {
        return item == null || item.getId() == null;
    }

    /**
     * To decode first string parameter from parameters array
     *
     * @param parameters input parameters
     * @return long value or throws exception
     * @see InvalidParameterTypeException
     */
    default String decodeStringArgument(final Object... parameters) {
        if (parameters == null || parameters.length < 1) {
            throw new IllegalArgumentException("Wrong number of parameters");
        }
        if (parameters[0] instanceof String value) {
            return value;
        } else {
            throw new InvalidParameterTypeException("String", parameters[0]);
        }
    }

    /**
     * To decode first long parameter from parameters array
     *
     * @param parameters input parameters
     * @return long value or throws exception
     * @see InvalidParameterTypeException
     */
    default Long decodeLongArgument(final Object... parameters) {
        if (parameters == null || parameters.length < 1) {
            throw new IllegalArgumentException("Wrong number of parameters");
        }
        if (parameters[0] instanceof Long value) {
            return value;
        } else {
            throw new InvalidParameterTypeException("Long", parameters[0]);
        }
    }
}
