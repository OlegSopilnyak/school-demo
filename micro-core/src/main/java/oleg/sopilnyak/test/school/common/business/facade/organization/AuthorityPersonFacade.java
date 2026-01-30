package oleg.sopilnyak.test.school.common.business.facade.organization;

import oleg.sopilnyak.test.school.common.business.facade.BusinessFacade;
import oleg.sopilnyak.test.school.common.business.facade.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonManagesFacultyException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Service-Facade: Service for manage organization in the school (authority persons)
 *
 * @see OrganizationFacade
 * @see AuthorityPerson
 */
public interface AuthorityPersonFacade extends OrganizationFacade, BusinessFacade {
    //
    // action-ids
    String PREFIX = "organization.authority.person";
    String LOGIN = PREFIX + ".login";
    String LOGOUT = PREFIX + ".logout";
    String FIND_ALL = PREFIX + ".findAll";
    String FIND_BY_ID = PREFIX + ".findById";
    String CREATE_OR_UPDATE = PREFIX + ".createOrUpdate";
    String CREATE_NEW = PREFIX + ".create.Macro";
    String DELETE = PREFIX + ".delete";
    String DELETE_ALL = PREFIX + ".delete.Macro";
    //
    // the list of valid action-ids
    List<String> ACTION_IDS = List.of(LOGIN, LOGOUT, FIND_ALL, FIND_BY_ID, CREATE_OR_UPDATE, CREATE_NEW, DELETE, DELETE_ALL);

    /**
     * To get the list of valid action-ids
     *
     * @return valid action-ids for concrete descendant-facade
     */
    @Override
    default List<String> validActions() {
        return ACTION_IDS;
    }

    /**
     * To get the name of the facade
     *
     * @return facade's name
     */
    @Override
    default String getName() {
        return "AuthorityPersonFacade";
    }

    /**
     * To log in AuthorityPerson by it valid login and password
     *
     * @param username the value of person's username (login)
     * @param password the value of person's password
     * @return logged in person's instance or exception will be thrown
     * @deprecated
     */
    @Deprecated
    Optional<AuthorityPerson> login(String username, String password);

    /**
     * To log out the person
     *
     * @param token logged in person's authorization token (see Authorization: Bearer <token>)
     * @deprecated
     */
    @Deprecated
    void logout(String token);

    /**
     * To get all authorityPerson
     *
     * @return list of persons
     * @see AuthorityPerson
     * @deprecated
     */
    @Deprecated
    Collection<AuthorityPerson> findAllAuthorityPersons();

    /**
     * To get the authorityPerson by ID
     *
     * @param id system-id of the authorityPerson
     * @return AuthorityPerson instance or empty() if not exists
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     * @deprecated
     */
    @Deprecated
    Optional<AuthorityPerson> findAuthorityPersonById(Long id);

    /**
     * To create or update authorityPerson instance
     *
     * @param instance authorityPerson should be created or updated
     * @return AuthorityPerson instance or empty() if it cannot do
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     * @deprecated
     */
    @Deprecated
    Optional<AuthorityPerson> createOrUpdateAuthorityPerson(AuthorityPerson instance);

    /**
     * To create person instance + it's profile
     *
     * @param instance person should be created
     * @return person instance or empty() if it cannot do
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     * @deprecated
     */
    @Deprecated
    Optional<AuthorityPerson> create(AuthorityPerson instance);

    /**
     * To delete authorityPerson from the school
     *
     * @param id system-id of the authorityPerson to delete
     * @throws AuthorityPersonNotFoundException       throws when authorityPerson is not exists
     * @throws AuthorityPersonManagesFacultyException throws when authorityPerson takes place in a faculty as a dean
     * @deprecated
     */
    @Deprecated
    void deleteAuthorityPersonById(Long id)
            throws AuthorityPersonNotFoundException,
            AuthorityPersonManagesFacultyException;

    @Deprecated
    default void deleteAuthorityPerson(AuthorityPerson instance)
            throws AuthorityPersonNotFoundException,
            AuthorityPersonManagesFacultyException {
        if (isInvalid(instance)) {
            throw new AuthorityPersonNotFoundException("Wrong " + instance + " to delete");
        }
        deleteAuthorityPersonById(instance.getId());
    }
}
