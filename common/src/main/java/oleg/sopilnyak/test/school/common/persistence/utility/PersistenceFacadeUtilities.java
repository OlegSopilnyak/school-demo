package oleg.sopilnyak.test.school.common.persistence.utility;

import static java.util.Objects.isNull;

/**
 * Class-Utility for persistence facades
 */
public interface PersistenceFacadeUtilities {

    /**
     * To check is instanceId has valid value
     *
     * @param instanceId system-id of the entity
     * @return true if invalid
     */
    static boolean isInvalidId(final Long instanceId) {
        return isNull(instanceId) || instanceId <= 0L;
    }
}
