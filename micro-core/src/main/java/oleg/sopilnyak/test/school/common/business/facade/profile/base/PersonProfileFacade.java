package oleg.sopilnyak.test.school.common.business.facade.profile.base;

import oleg.sopilnyak.test.school.common.business.facade.BusinessFacade;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.model.person.profile.PersonProfile;

/**
 * Service-BaseFacade: Service for manage person profiles in the school
 *
 */
public interface PersonProfileFacade extends BusinessFacade {
    String NAMESPACE = "school::person::profile";

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

    /**
     * To convert argument to particular type (long)
     *
     * @param argument value to convert
     * @return converted value or throws InvalidParameterTypeException
     * @see InvalidParameterTypeException
     */
    default Long toLong(final Object argument) {
        if (argument instanceof Long id) {
            return id;
        } else {
            throw new InvalidParameterTypeException("Long", argument);
        }
    }

    /**
     * To convert argument to particular type (person profile)
     *
     * @param argument value to convert
     * @return converted value or throws InvalidParameterTypeException
     * @see InvalidParameterTypeException
     */
    default PersonProfile toPersonProfile(final Object argument) {
        if (argument instanceof PersonProfile profile) {
            return profile;
        } else {
            throw new InvalidParameterTypeException("PersonProfile", argument);
        }
    }
}
