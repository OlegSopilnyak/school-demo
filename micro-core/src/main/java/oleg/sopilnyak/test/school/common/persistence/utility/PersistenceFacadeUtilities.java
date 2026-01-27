package oleg.sopilnyak.test.school.common.persistence.utility;

import oleg.sopilnyak.test.school.common.model.BaseType;

/**
 * Class-Utility for persistence facades
 */
public interface PersistenceFacadeUtilities {
    /**
     * To check is entity input is invalid
     *
     * @param instance input entity to check
     * @return true if invalid
     */
    static boolean isInvalid(final BaseType instance) {
        return instance == null || isInvalidId(instance.getId());
    }

    /**
     * To check is instanceId has valid value
     *
     * @param id system-id of the entity
     * @return true if invalid
     */
    static boolean isInvalidId(final Long id) {
        return id == null || id <= 0L;
    }
}
