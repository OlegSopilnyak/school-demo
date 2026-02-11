package oleg.sopilnyak.test.school.common.persistence.organization;

import static java.util.Objects.nonNull;

import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonManagesFacultyException;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;

import java.util.Optional;
import java.util.Set;

/**
 * Persistence facade for organization structure entities (authority persons)
 *
 * @see AuthorityPerson
 */
public interface AuthorityPersonPersistenceFacade {
    /**
     * To get all authority persons of the school
     *
     * @return the set of authority persons
     * @see AuthorityPerson
     */
    Set<AuthorityPerson> findAllAuthorityPersons();

    /**
     * To find authority person by id
     *
     * @param id system-id of the authority person
     * @return authority person instance or empty() if not exists
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    Optional<AuthorityPerson> findAuthorityPersonById(Long id);

    /**
     * To find authority person by profile-id
     *
     * @param id system-id of the profile of the authority person
     * @return authority person instance or empty() if not exists
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    Optional<AuthorityPerson> findAuthorityPersonByProfileId(Long id);

    /**
     * Create or update authority person
     *
     * @param person authority person instance to store
     * @return authority person instance or empty(), if instance couldn't store
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    Optional<AuthorityPerson> save(AuthorityPerson person);

    /**
     * To delete authority person by id
     *
     * @param id system-id of the authority person
     * @throws AuthorityPersonManagesFacultyException throws when you want to delete authority person who is the dean of a faculty now
     * @throws AuthorityPersonNotFoundException   throws when you want to delete authority person who is not created before
     * @return true if success
     * @see AuthorityPerson
     */
    boolean deleteAuthorityPerson(Long id) throws
            AuthorityPersonManagesFacultyException,
            AuthorityPersonNotFoundException;

    /**
     * To update authority person's access parameters
     *
     * @param person   the instance of authority person to update
     * @param username new value of login's username
     * @param password new value of login's password
     * @return true if changes applied
     */
    default boolean updateAccess(AuthorityPerson person, String username, String password) {
        return isValid(person) && updateAccess(person.getId(), username, password);
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
    boolean updateAccess(Long personId, String username, String password);

}
