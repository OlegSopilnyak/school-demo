package oleg.sopilnyak.test.school.common.persistence;

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
