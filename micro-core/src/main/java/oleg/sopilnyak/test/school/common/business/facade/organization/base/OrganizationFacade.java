package oleg.sopilnyak.test.school.common.business.facade.organization.base;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.school.common.business.facade.BusinessFacade;
import oleg.sopilnyak.test.school.common.model.BaseType;

/**
 * Service-Facade: Service for manage organization in the school
 *
 * @see BaseType
 */
public interface OrganizationFacade extends BusinessFacade {
    // actions name-space
    String NAMESPACE = "school::organization::authority::person";

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
        return organizationAction(actionId, parameters);
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
    default <T> T organizationAction(String actionId, Object... parameters) {
        throw new UnsupportedOperationException("Please implement method in OrganizationFacade's descendant.");
    }

    /**
     * To check the state of model's item
     *
     * @param item instance to check
     * @return true if instance is invalid (empty or has invalid system-id)
     */
    default boolean isInvalid(BaseType item) {
        return isNull(item) || isNull(item.getId());
    }
}
