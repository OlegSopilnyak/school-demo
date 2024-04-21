package oleg.sopilnyak.test.school.common.persistence;

import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.organization.FacultyPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;

/**
 * Persistence facade for organization structure entities
 *
 * @see AuthorityPersonPersistenceFacade
 * @see FacultyPersistenceFacade
 * @see StudentsGroupPersistenceFacade
 */
public interface OrganizationPersistenceFacade
        extends AuthorityPersonPersistenceFacade, FacultyPersistenceFacade, StudentsGroupPersistenceFacade {
}
