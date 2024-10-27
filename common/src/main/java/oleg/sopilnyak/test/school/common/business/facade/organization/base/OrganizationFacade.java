package oleg.sopilnyak.test.school.common.business.facade.organization.base;

import oleg.sopilnyak.test.school.common.model.base.BaseType;

import static java.util.Objects.isNull;

/**
 * Service-Facade: Service for manage organization in the school
 *
 * @see BaseType
 */
public interface OrganizationFacade {
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
