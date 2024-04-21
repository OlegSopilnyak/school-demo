package oleg.sopilnyak.test.school.common.business.organization;

import oleg.sopilnyak.test.school.common.exception.AuthorityPersonIsNotExistsException;
import oleg.sopilnyak.test.school.common.exception.AuthorityPersonManageFacultyException;
import oleg.sopilnyak.test.school.common.business.OrganizationFacade;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;

import java.util.Collection;
import java.util.Optional;

/**
 * Service-Facade: Service for manage organization in the school (authority persons)
 *
 * @see OrganizationFacade
 * @see AuthorityPerson
 */
public interface AuthorityPersonFacade extends OrganizationFacade {
    /**
     * To get all authorityPerson
     *
     * @return list of persons
     * @see AuthorityPerson
     */
    Collection<AuthorityPerson> findAllAuthorityPersons();

    /**
     * To get the authorityPerson by ID
     *
     * @param id system-id of the authorityPerson
     * @return AuthorityPerson instance or empty() if not exists
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    Optional<AuthorityPerson> getAuthorityPersonById(Long id);

    /**
     * To create or update authorityPerson instance
     *
     * @param instance authorityPerson should be created or updated
     * @return AuthorityPerson instance or empty() if not exists
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    Optional<AuthorityPerson> createOrUpdateAuthorityPerson(AuthorityPerson instance);

    /**
     * To delete authorityPerson from the school
     *
     * @param id system-id of the authorityPerson to delete
     * @throws AuthorityPersonIsNotExistsException   throws when authorityPerson is not exists
     * @throws AuthorityPersonManageFacultyException throws when authorityPerson takes place in a faculty as a dean
     */
    void deleteAuthorityPersonById(Long id)
            throws AuthorityPersonIsNotExistsException,
            AuthorityPersonManageFacultyException;

    default void deleteAuthorityPerson(AuthorityPerson instance)
            throws AuthorityPersonIsNotExistsException,
            AuthorityPersonManageFacultyException {
        if (isInvalid(instance)) {
            throw new AuthorityPersonIsNotExistsException("Wrong " + instance + " to delete");
        }
        deleteAuthorityPersonById(instance.getId());
    }
}