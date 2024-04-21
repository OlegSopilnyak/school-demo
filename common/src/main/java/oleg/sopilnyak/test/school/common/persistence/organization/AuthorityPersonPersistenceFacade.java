package oleg.sopilnyak.test.school.common.persistence.organization;

import oleg.sopilnyak.test.school.common.exception.AuthorityPersonIsNotExistsException;
import oleg.sopilnyak.test.school.common.exception.AuthorityPersonManageFacultyException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;

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
     * Create or update authority person
     *
     * @param authorityPerson authority person instance to store
     * @return authority person instance or empty(), if instance couldn't store
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    Optional<AuthorityPerson> save(AuthorityPerson authorityPerson);

    /**
     * To delete authority person by id
     *
     * @param id system-id of the authority person
     * @throws AuthorityPersonManageFacultyException throws when you want to delete authority person who is the dean of a faculty now
     * @throws AuthorityPersonIsNotExistsException   throws when you want to delete authority person who is not created before
     * @see AuthorityPerson
     */
    void deleteAuthorityPerson(Long id) throws
            AuthorityPersonManageFacultyException,
            AuthorityPersonIsNotExistsException;

}
