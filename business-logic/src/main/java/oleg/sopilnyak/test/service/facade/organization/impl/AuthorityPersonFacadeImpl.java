package oleg.sopilnyak.test.service.facade.organization.impl;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.business.facade.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.exception.accsess.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonManagesFacultyException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.facade.organization.base.impl.OrganizationFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;

import java.util.Collection;
import java.util.Optional;
import java.util.function.UnaryOperator;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.*;
import static oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand.*;


/**
 * Service-Facade: Service for manage organization in the school (authority persons)
 *
 * @see OrganizationFacade
 * @see OrganizationFacadeImpl
 * @see AuthorityPerson
 * @see AuthorityPersonCommand
 */
@Slf4j
public class AuthorityPersonFacadeImpl extends OrganizationFacadeImpl<AuthorityPersonCommand<?>> implements AuthorityPersonFacade {
    private final BusinessMessagePayloadMapper mapper;
    // semantic data to payload converter
    private final UnaryOperator<AuthorityPerson> convert;

    public AuthorityPersonFacadeImpl(final CommandsFactory<AuthorityPersonCommand<?>> factory,
                                     final BusinessMessagePayloadMapper mapper) {
        super(factory);
        this.mapper = mapper;
        this.convert = person -> person instanceof AuthorityPersonPayload ? person : this.mapper.toPayload(person);
    }

    /**
     * To log in AuthorityPerson by it valid login and password
     *
     * @param username the value of person's username (login)
     * @param password the value of person's password
     * @return logged in person's instance or exception will be thrown
     * @see AuthorityPersonCommand#LOGIN
     */
    @Override
    public Optional<AuthorityPerson> login(String username, String password) {
        log.debug("Try to login using: '{}'", username);
        final String commandId = LOGIN;
        final AuthorityPersonCommand<Optional<AuthorityPerson>> command =
                (AuthorityPersonCommand<Optional<AuthorityPerson>>) takeValidCommand(commandId, factory);
        final Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(username, password));

        command.doCommand(context);

        if (context.isDone()) {
            // success processing
            log.debug("Logged in authority person with login: '{}' successfully.", username);
            final Optional<AuthorityPerson> loggedIn = context.getResult().orElseThrow(createThrowFor(command.getId()));
            log.debug("Person is logged in: {}", loggedIn);
            return loggedIn;
        }
        // fail processing
        final Exception loginException = context.getException();
        log.warn(SOMETHING_WENT_WRONG, loginException);
        if (loginException instanceof ProfileNotFoundException notFoundException) {
            throw notFoundException;
        } else if (loginException instanceof SchoolAccessDeniedException accessIsDeniedException) {
            throw accessIsDeniedException;
        } else if (nonNull(loginException)) {
            throwFor(commandId, loginException);
        } else {
            wrongCommandExecution(LOGIN);
        }
        return Optional.empty();
    }

    /**
     * To log out the AuthorityPerson
     *
     * @param token logged in person's authorization token (see Authorization: Bearer <token>)
     * @see AuthorityPersonCommand#LOGIN
     */
    @Override
    public void logout(String token) {
        log.debug("Logout for token: {}", token);
        final boolean loggedOut = doSimpleCommand(LOGOUT, Input.of(token), factory);
        log.debug("Person is logged out: {}", loggedOut);
    }

    /**
     * To get all authorityPerson
     *
     * @return list of persons
     * @see AuthorityPerson
     * @see AuthorityPersonPayload
     * @see AuthorityPersonCommand#FIND_ALL
     */
    @Override
    public Collection<AuthorityPerson> findAllAuthorityPersons() {
        log.debug("Find all authority persons");
        final Collection<AuthorityPerson> result = doSimpleCommand(FIND_ALL, null, factory);
        log.debug("Found all authority persons {}", result);
        return result.stream().map(convert).toList();
    }

    /**
     * To get the authorityPerson by ID
     *
     * @param id system-id of the authorityPerson
     * @return AuthorityPerson instance or empty() if not exists
     * @see AuthorityPerson
     * @see AuthorityPersonPayload
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<AuthorityPerson> findAuthorityPersonById(Long id) {
        log.debug("Find authority person by ID:{}", id);
        final Optional<AuthorityPerson> result = doSimpleCommand(FIND_BY_ID, Input.of(id), factory);
        log.debug("Found authority person {}", result);
        return result.map(convert);
    }

    /**
     * To create or update authorityPerson instance
     *
     * @param instance authorityPerson should be created or updated
     * @return AuthorityPerson instance or empty() if not exists
     * @see AuthorityPerson
     * @see AuthorityPersonPayload
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<AuthorityPerson> createOrUpdateAuthorityPerson(AuthorityPerson instance) {
        log.debug("Create or Update authority person {}", instance);
        final Optional<AuthorityPerson> result = doSimpleCommand(CREATE_OR_UPDATE, Input.of(convert.apply(instance)), factory);
        log.debug("Changed authority person {}", result);
        return result.map(convert);
    }

    /**
     * To create person instance + it's profile
     *
     * @param instance person should be created
     * @return person instance or empty() if it cannot do
     * @see AuthorityPerson
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<AuthorityPerson> create(AuthorityPerson instance) {
        log.debug("Create authority person with new profile {}", instance);
        final Optional<AuthorityPerson> result = doSimpleCommand(CREATE_NEW, Input.of(convert.apply(instance)), factory);
        log.debug("Created authority person {}", result);
        return result.map(convert);
    }

    /**
     * To delete authorityPerson from the school
     *
     * @param id system-id of the authorityPerson to delete
     * @throws AuthorityPersonNotFoundException       throws when authorityPerson is not exists
     * @throws AuthorityPersonManagesFacultyException throws when authorityPerson takes place in a faculty as a dean
     */
    @Override
    public void deleteAuthorityPersonById(Long id) throws AuthorityPersonNotFoundException, AuthorityPersonManagesFacultyException {
        log.debug("Delete authority person with ID:{}", id);
        final String commandId = DELETE_ALL;
        final RootCommand<Boolean> command = (RootCommand<Boolean>) takeValidCommand(commandId, factory);
        final Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        if (context.isDone()) {
            // success processing
            log.debug("Deleted authority person with ID:{} successfully.", id);
            return;
        }

        // fail processing
        final Exception deleteException = context.getException();
        log.warn(SOMETHING_WENT_WRONG, deleteException);
        if (deleteException instanceof AuthorityPersonNotFoundException noPersonException) {
            throw noPersonException;
        } else if (deleteException instanceof AuthorityPersonManagesFacultyException exception) {
            throw exception;
        } else if (nonNull(deleteException)) {
            throwFor(commandId, deleteException);
        } else {
            wrongCommandExecution(DELETE_ALL);
        }
    }

    // private methods
    private static void wrongCommandExecution(String commandId) {
        log.error(WRONG_COMMAND_EXECUTION, commandId);
        throwFor(commandId, new NullPointerException(EXCEPTION_IS_NOT_STORED));
    }
}
