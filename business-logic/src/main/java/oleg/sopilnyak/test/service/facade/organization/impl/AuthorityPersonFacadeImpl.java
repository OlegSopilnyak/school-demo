package oleg.sopilnyak.test.service.facade.organization.impl;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand.CommandId;

import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.business.facade.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.exception.accsess.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonManagesFacultyException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.facade.ActionFacade;
import oleg.sopilnyak.test.service.facade.organization.base.impl.OrganizationFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import lombok.extern.slf4j.Slf4j;


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
    // semantic data to payload converter
    private final UnaryOperator<AuthorityPerson> toPayload;

    public AuthorityPersonFacadeImpl(
            CommandsFactory<AuthorityPersonCommand<?>> factory,
            BusinessMessagePayloadMapper mapper,
            ActionExecutor actionExecutor
    ) {
        super(factory, actionExecutor);
        this.toPayload = person -> person instanceof AuthorityPersonPayload ? person : mapper.toPayload(person);
    }

    /**
     * To log in AuthorityPerson by it valid login and password
     *
     * @param username the value of person's username (login)
     * @param password the value of person's password
     * @return logged in person's instance or exception will be thrown
     * @see AuthorityPersonCommand.CommandId#LOGIN
     */
    @Override
    public Optional<AuthorityPerson> login(String username, String password) {
        final String commandId = CommandId.LOGIN;
        final Consumer<Exception> doThisOnError = exception -> {
            logSomethingWentWrong(exception, commandId);
            if (exception instanceof ProfileNotFoundException notFoundException) {
                throw notFoundException;
            } else if (exception instanceof SchoolAccessDeniedException accessIsDeniedException) {
                throw accessIsDeniedException;
            } else if (nonNull(exception)) {
                ActionFacade.throwFor(commandId, exception);
            } else {
                failedButNoExceptionStored(commandId);
            }
        };
        log.debug("Trying to log in using: '{}'", username);
        final var input = Input.of(username, password);
        final Optional<Optional<AuthorityPerson>> result = actCommand(commandId, factory, input, doThisOnError);
        if (result.isPresent()) {
            final Optional<AuthorityPerson> loggedIn = result.get();
            log.debug("Person is logged in: {}", loggedIn);
            return loggedIn.map(toPayload);
        }
        return Optional.empty();
    }

    /**
     * To log out the AuthorityPerson
     *
     * @param token logged in person's authorization token (see Authorization: Bearer <token>)
     * @see AuthorityPersonCommand.CommandId#LOGOUT
     */
    @Override
    public void logout(String token) {
        log.debug("Logging out person using token: {}", token);
        final Optional<Boolean> result = actCommand(CommandId.LOGOUT, factory, Input.of(token));
        result.ifPresent(executionResult ->
                log.debug("Person is logged out: {}", executionResult)
        );
    }

    /**
     * To get all authorityPerson
     *
     * @return list of persons
     * @see AuthorityPerson
     * @see AuthorityPersonPayload
     * @see AuthorityPersonCommand.CommandId#FIND_ALL
     */
    @Override
    public Collection<AuthorityPerson> findAllAuthorityPersons() {
        log.debug("Finding all authority persons");
        final Optional<Set<AuthorityPerson>> result;
        result = actCommand(CommandId.FIND_ALL, factory, Input.empty());
        if (result.isPresent()) {
            final Set<AuthorityPerson> personSet = result.get();
            log.debug("Found all authority persons {}", personSet);
            return personSet.stream().map(toPayload).collect(Collectors.toSet());
        }
        return Set.of();
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
        log.debug("Finding authority person by ID:{}", id);
        final Optional<Optional<AuthorityPerson>> result;
        result = actCommand(CommandId.FIND_BY_ID, factory, Input.of(id));
        if (result.isPresent()) {
            final Optional<AuthorityPerson> person = result.get();
            log.debug("Found authority person {}", person);
            return person.map(toPayload);
        }
        return Optional.empty();
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
        log.debug("Creating or Updating authority person {}", instance);
        final var input = Input.of(toPayload.apply(instance));
        final Optional<Optional<AuthorityPerson>> result;
        result = actCommand(CommandId.CREATE_OR_UPDATE, factory, input);
        if (result.isPresent()) {
            final Optional<AuthorityPerson> person = result.get();
            log.debug("Changed authority person {}", person);
            return person.map(toPayload);
        }
        return Optional.empty();
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
        log.debug("Creating authority person with new profile {}", instance);
        final var input = Input.of(toPayload.apply(instance));
        final Optional<Optional<AuthorityPerson>> result;
        result = actCommand(CommandId.CREATE_NEW, factory, input);
        if (result.isPresent()) {
            final Optional<AuthorityPerson> person = result.get();
            log.debug("Created authority person {}", person);
            return person.map(toPayload);
        }
        return Optional.empty();
    }

    /**
     * To delete authorityPerson from the school (with profile at once)
     *
     * @param id system-id of the authorityPerson to delete
     * @throws AuthorityPersonNotFoundException       throws when authorityPerson is not exists
     * @throws AuthorityPersonManagesFacultyException throws when authorityPerson takes place in a faculty as a dean
     */
    @Override
    public void deleteAuthorityPersonById(Long id) throws AuthorityPersonNotFoundException, AuthorityPersonManagesFacultyException {
        final String commandId = CommandId.DELETE_ALL;
        final Consumer<Exception> doThisOnError = exception -> {
            logSomethingWentWrong(exception, commandId);
            if (exception instanceof AuthorityPersonNotFoundException noPersonException) {
                throw noPersonException;
            } else if (exception instanceof AuthorityPersonManagesFacultyException managesFacultyException) {
                throw managesFacultyException;
            } else if (nonNull(exception)) {
                ActionFacade.throwFor(commandId, exception);
            } else {
                failedButNoExceptionStored(commandId);
            }
        };
        log.debug("Deleting authority person with ID:{} and it's profile", id);
        final Optional<Boolean> result = actCommand(commandId, factory, Input.of(id), doThisOnError);
        result.ifPresent(executionResult ->
                log.debug("Deleted authority person with ID:{} successfully:{} .", id, executionResult)
        );
    }

    /**
     * To get the logger of the facade
     *
     * @return logger instance
     */
    @Override
    public Logger getLogger() {
        return log;
    }

    // private methods
}
