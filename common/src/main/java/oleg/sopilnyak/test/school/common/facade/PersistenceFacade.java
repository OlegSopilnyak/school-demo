package oleg.sopilnyak.test.school.common.facade;

import oleg.sopilnyak.test.school.common.facade.peristence.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.facade.peristence.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.facade.peristence.RegisterPersistenceFacade;
import oleg.sopilnyak.test.school.common.facade.peristence.StudentsPersistenceFacade;

/**
 * Service-Facade: Service for manage persistence layer of the school
 */
public interface PersistenceFacade extends
        StudentsPersistenceFacade,
        CoursesPersistenceFacade,
        RegisterPersistenceFacade,
        OrganizationPersistenceFacade
{
    /**
     * To initialize default minimal data-set for the application
     */
    void initDefaultDataset();

}
