package oleg.sopilnyak.test.service.facade.organization.impl;

import static oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand.CommandId;

import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.business.facade.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.exception.accsess.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonManagesFacultyException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.facade.organization.base.impl.OrganizationFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
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
    public static final String LOGIN_KEY = "login";
    public static final String PASSWORD_KEY = "password";
    // semantic data to payload converter
    private final UnaryOperator<AuthorityPerson> toPayload;
    //
    // setting up action-methods by action-id
    private final Map<String, Function<Object[], Object>> actions = Map.<String,  Function<Object[], Object>>of(
            AuthorityPersonFacade.LOGIN, this::internalLogin
    ).entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey, Map.Entry::getValue,
                    (existing, _) -> existing,
                    HashMap::new
            )
    );

    public AuthorityPersonFacadeImpl(
            CommandsFactory<AuthorityPersonCommand<?>> factory,
            BusinessMessagePayloadMapper mapper,
            CommandActionExecutor actionExecutor
    ) {
        super(factory, actionExecutor);
        this.toPayload = person -> person instanceof AuthorityPersonPayload ? person : mapper.toPayload(person);
    }

    /**
     * Facade depends on the action's execution (organization action)
     *
     * @param actionId   the id of the action
     * @param parameters the parameters of the action to execute
     * @return action execution result value
     * @see OrganizationFacade#doActionAndResult(String, Object...)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T organizationAction(final String actionId, final Object... parameters) {
        getLogger().debug("Trying to execute action {} with arguments {}", actionId, parameters);
        return (T) actions.computeIfAbsent(actionId, this::throwsUnknownActionId).apply(parameters);
    }

    /**
     * To log in AuthorityPerson by it valid login and password
     *
     * @param username the value of person's username (login)
     * @param password the value of person's password
     * @return logged in person's instance or exception will be thrown
     * @see AuthorityPersonFacade#LOGIN
     * @deprecated
     */
    @Deprecated
    @Override
    public Optional<AuthorityPerson> login(String username, String password) {
        final String commandId = CommandId.LOGIN;
        final Consumer<Exception> doThisOnError = exception -> {
            switch (exception) {
                case ProfileNotFoundException notFoundException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw notFoundException;
                }
                case SchoolAccessDeniedException accessIsDeniedException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw accessIsDeniedException;
                }
                case null, default -> defaultDoOnError(commandId).accept(exception);
            }
        };

        log.debug("Trying to log in using: '{}'", username);
        final var input = Input.of(username, password);
        final Optional<Optional<AuthorityPerson>> result = executeCommand(commandId, factory, input, doThisOnError);
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
        final Optional<Boolean> result = executeCommand(CommandId.LOGOUT, factory, Input.of(token));
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
        result = executeCommand(CommandId.FIND_ALL, factory, Input.empty());
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
        result = executeCommand(CommandId.FIND_BY_ID, factory, Input.of(id));
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
        result = executeCommand(CommandId.CREATE_OR_UPDATE, factory, input);
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
        result = executeCommand(CommandId.CREATE_NEW, factory, input);
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
            switch (exception) {
                case AuthorityPersonNotFoundException noPersonException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw noPersonException;
                }
                case AuthorityPersonManagesFacultyException managesFacultyException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw managesFacultyException;
                }
                case null, default -> defaultDoOnError(commandId).accept(exception);
            }
        };

        log.debug("Deleting authority person with ID:{} and it's profile", id);
        final Optional<Boolean> result = executeCommand(commandId, factory, Input.of(id), doThisOnError);
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
    // throws exception if action-id is invalid
    private Function<Object[], Object> throwsUnknownActionId(final String actionId) {
        final String expectedTypes = String.join(" or ", validActions());
        throw new InvalidParameterTypeException(expectedTypes, actionId);
    }

    // to decode logi/password from parameters array
    private static Map<String, String> decodeAuthentication(final Object... parameters) {
        if (parameters.length != 2) {
            throw new IllegalArgumentException("Wrong number of parameters for the login action");
        }
        final Map<String, String> result = new HashMap<>();
        if (parameters[0] instanceof String login) {
            result.put(LOGIN_KEY, login);
        } else {
            throw new InvalidParameterTypeException("String", parameters[0]);
        }
        if (parameters[1] instanceof String password) {
            result.put(PASSWORD_KEY, password);
        } else {
            throw new InvalidParameterTypeException("String", parameters[1]);
        }
        return result;
    }

    // To log in AuthorityPerson by it valid login and password
    private Optional<AuthorityPerson> internalLogin(final Object... parameters) {
        final Map<String, String> authentication = decodeAuthentication(parameters);
        final String username = authentication.get(LOGIN_KEY);
        final String password = authentication.get(PASSWORD_KEY);
        // doing login action business logic
        final String commandId = AuthorityPersonFacade.LOGIN;
        // declaring custom error handler
        final Consumer<Exception> doOnError = exception -> {
            switch (exception) {
                case ProfileNotFoundException notFoundException -> {
                    // profile of person with username is not exists in the database
                    logSomethingWentWrong(exception, commandId);
                    throw notFoundException;
                }
                case SchoolAccessDeniedException accessIsDeniedException -> {
                    // access is denied for person with username
                    logSomethingWentWrong(exception, commandId);
                    throw accessIsDeniedException;
                }
                case null, default -> defaultDoOnError(commandId).accept(exception);
            }
        };

        log.debug("Trying to log in person using: '{}'", username);
        final var input = Input.of(username, password);
        final Optional<Optional<AuthorityPerson>> result = executeCommand(commandId, factory, input, doOnError);
        return result.flatMap(person -> {
            log.debug("AuthorityPerson is logged in: {}", person);
            return person.map(toPayload);
        });
    }
}
