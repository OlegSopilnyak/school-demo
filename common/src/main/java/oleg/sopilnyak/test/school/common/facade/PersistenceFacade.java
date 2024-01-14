package oleg.sopilnyak.test.school.common.facade;

import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.facade.peristence.StudentCourseLinkPersistenceFacade;

/**
 * Service-Facade: Service for manage persistence layer of the school
 */
public interface PersistenceFacade extends
        StudentCourseLinkPersistenceFacade,
        ProfilePersistenceFacade,
        OrganizationPersistenceFacade {
    /**
     * To initialize default minimal data-set for the application
     */
    void initDefaultDataset();

}
