package oleg.sopilnyak.test.school.common.persistence;

import oleg.sopilnyak.test.school.common.model.AuthorityPerson;

import static java.util.Objects.isNull;

/**
 * Service-Facade: Service for manage persistence layer of the school
 */
public interface PersistenceFacade extends
        EducationPersistenceFacade,
        ProfilePersistenceFacade,
        OrganizationPersistenceFacade {
    /**
     * To initialize default minimal data-set for the application
     */
    void initDefaultDataset();

    /**
     * To update authority person's access parameters
     *
     * @param person the instance of authority person
     * @param username new value of login's username
     * @param password new value of login's password
     * @return  true if changes applied
     */
    default boolean updateAuthorityPersonAccess(AuthorityPerson person, String username, String password){
        if (isNull(person) || isNull(person.getId()) || Long.signum(person.getId()) <= 0L ) {
            return false;
        }
        return updateAuthorityPersonAccess(person.getId(), username, password);
    }
    /**
     * To update authority person's access parameters
     *
     * @param personId system-id of authority person
     * @param username new value of login's username
     * @param password new value of login's password
     * @return  true if changes applied
     */
    boolean updateAuthorityPersonAccess(Long personId, String username, String password);

}
