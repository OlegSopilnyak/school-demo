package oleg.sopilnyak.test.school.common.business.facade.education.base;


import oleg.sopilnyak.test.school.common.business.facade.BusinessFacade;
import oleg.sopilnyak.test.school.common.business.facade.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.model.BaseType;

import java.util.function.Function;

/**
 * Service-Facade: Service for manage education courses and students in the school
 *
 * @see BaseType
 */
public interface EducationFacade extends BusinessFacade {
    // actions name-space
    String NAMESPACE = "school::education";

    /**
     * Unified facade's entry-point to do action and return the result
     * delegate to facade's main entry-point method
     *
     * @param actionId   the id of the action
     * @param parameters the parameters of the action to execute
     * @return action execution result value
     * @see OrganizationFacade#organizationAction(String, Object...)
     */
    @Override
    default <T> T doActionAndResult(final String actionId, final Object... parameters) {
        return educationAction(actionId, parameters);
    }

    /**
     * Facade depends on the action's execution (organization action)
     *
     * @param actionId   the id of the action
     * @param parameters the parameters of the action to execute
     * @param <T>        type of action execution result
     * @return action execution result value
     * @see BusinessFacade#doActionAndResult(String, Object...)
     */
    default <T> T educationAction(String actionId, Object... parameters) {
        throw new UnsupportedOperationException("Please implement method in EducationFacade's descendant.");
    }

    /**
     * Throws exception if action-id is invalid
     *
     * @param actionId   the id of the action
     * @return nothing
     * @see BusinessFacade#throwInvalidActionId(String)
     */
    default Function<Object[], Object> throwsUnknownActionId(final String actionId) {
        throwInvalidActionId(actionId);
        return null;
    }
}
