package oleg.sopilnyak.test.school.common.persistence;

import oleg.sopilnyak.test.school.common.persistence.education.joint.EducationPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.organization.joint.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;

/**
 * Service-Facade: Service for manage persistence layer of the school application
 */
public interface PersistenceFacade
        extends EducationPersistenceFacade, OrganizationPersistenceFacade, ProfilePersistenceFacade {
    /**
     * To initialize default minimal data-set for the application
     */
    void initDefaultDataset();
}
