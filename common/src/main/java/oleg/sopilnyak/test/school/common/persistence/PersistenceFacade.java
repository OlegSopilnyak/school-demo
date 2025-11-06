package oleg.sopilnyak.test.school.common.persistence;

import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.education.joint.EducationPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.organization.joint.OrganizationPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;

import static java.util.Objects.nonNull;

/**
 * Service-Facade: Service for manage persistence layer of the school application
 */
public interface PersistenceFacade
        extends EducationPersistenceFacade, OrganizationPersistenceFacade, ProfilePersistenceFacade {
    /**
     * To initialize default minimal data-set for the application
     */
    void initDefaultDataset();

    /**
     * To update authority person's access parameters
     *
     * @param person   the instance of authority person to update
     * @param username new value of login's username
     * @param password new value of login's password
     * @return true if changes applied
     */
    default boolean updateAuthorityPersonAccess(AuthorityPerson person, String username, String password) {
        return isValid(person) && updateAuthorityPersonAccess(person.getId(), username, password);
    }

    private static boolean isValid(final AuthorityPerson person) {
        return nonNull(person) && nonNull(person.getId()) && person.getId() > 0L;
    }

    /**
     * To update authority person's access parameters
     *
     * @param personId system-id of authority person to update
     * @param username new value of login's username
     * @param password new value of login's password
     * @return true if changes applied
     */
    boolean updateAuthorityPersonAccess(Long personId, String username, String password);

}
