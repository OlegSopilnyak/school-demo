package oleg.sopilnyak.test.service.facade.organization.impl;

import oleg.sopilnyak.test.school.common.business.facade.organization.AuthorityPersonFacade;
import oleg.sopilnyak.test.school.common.business.facade.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonManagesFacultyException;
import oleg.sopilnyak.test.school.common.exception.organization.AuthorityPersonNotFoundException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
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
 * @see AuthorityPersonFacade
 * @see AccessCredentials
 */
@Slf4j
public class AuthorityPersonFacadeImpl extends OrganizationFacadeImpl<AuthorityPersonCommand<?>> implements AuthorityPersonFacade {
    private static final String LOGIN_KEY = "login";
    private static final String PASSWORD_KEY = "password";
    // semantic data to payload converter
    private final UnaryOperator<AuthorityPerson> toPayload;

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
        getLogger().debug("Trying to execute action [{}] with arguments: {}", actionId, parameters);
        return (T) switch (actionId) {
            case AuthorityPersonFacade.LOGIN -> internalLogin(parameters);
            case AuthorityPersonFacade.LOGOUT -> internalLogout(parameters);
            case AuthorityPersonFacade.FIND_ALL -> internalFindAll();
            case AuthorityPersonFacade.FIND_BY_ID -> internalFindById(parameters);
            case AuthorityPersonFacade.CREATE_OR_UPDATE -> internalCreateOrUpdate(parameters);
            case AuthorityPersonFacade.CREATE_MACRO -> internalCreateComposite(parameters);
            case AuthorityPersonFacade.DELETE_MACRO -> internalDeleteComposite(parameters);
            default -> throwsUnknownActionId(actionId).apply(null);
        };
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
    // to decode login/password from parameters array
    private static Map<String, String> decodeAuthentication(final Object... parameters) {
        if (parameters == null || parameters.length != 2) {
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

    // to decode authority person from parameters array
    private static AuthorityPerson decodePersonArgument(final Object... parameters) {
        if (parameters == null || parameters.length < 1) {
            throw new IllegalArgumentException("Wrong number of parameters");
        }
        if (parameters[0] instanceof AuthorityPerson value) {
            return value;
        } else {
            throw new InvalidParameterTypeException("AuthorityPerson", parameters[0]);
        }
    }

    // To log in AuthorityPerson by it valid login and password (for entry-point)
    private Optional<AccessCredentials> internalLogin(final Object... parameters) {
        final Map<String, String> auth = decodeAuthentication(parameters);
        return internalLogin(auth.get(LOGIN_KEY), auth.get(PASSWORD_KEY));
    }

    // To log in AuthorityPerson by it valid username and password (for internal usage)
    private Optional<AccessCredentials> internalLogin(final String username, final String password) {
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
        //
        // logging in the person
        log.debug("Trying to log in person using: '{}'", username);
        final var input = Input.of(username, password);
        final Optional<Optional<AccessCredentials>> result = executeCommand(commandId, factory, input, doOnError);
        return result.flatMap(credentials -> {
            // logged in successfully
            log.debug("AuthorityPerson with username: '{}' is signed in.", username);
            return credentials;
        });
    }

    // To log out the AuthorityPerson (for entry-point)
    private Optional<AccessCredentials> internalLogout(final Object... parameters) {
        return internalLogout(decodeStringArgument(parameters));
    }

    // To log out the AuthorityPerson (for internal usage)
    private Optional<AccessCredentials> internalLogout(final String token) {
        log.debug("Logging out person using token: {}", token);
        final Optional<Optional<AccessCredentials>> result = executeCommand(AuthorityPersonFacade.LOGOUT, factory, Input.of(token));
        result.ifPresent(executionResult ->
                log.debug("Person is logged out: {}", executionResult)
        );
        return result.flatMap(credentials -> {
            // logged in successfully
            log.debug("AuthorityPerson is signed out with former credentials: '{}'", credentials);
            return credentials;
        });
    }

    // To get all authority persons (for entry-point)
    // To get all authority persons (for internal usage)
    private Collection<AuthorityPerson> internalFindAll() {
        log.debug("Finding all authority persons");
        final Optional<Set<AuthorityPerson>> result = executeCommand(AuthorityPersonFacade.FIND_ALL, factory, Input.empty());
        return result.map(entities -> {
            log.debug("Found all authority persons {}", entities);
            return entities.stream().map(toPayload).collect(Collectors.toSet());
        }).orElseGet(Set::of);
    }

    // To get the authority person by ID (for entry-point)
    private Optional<AuthorityPerson> internalFindById(final Object... parameters) {
        return internalFindById(decodeLongArgument(parameters));
    }

    // To get the authority person by ID (for internal usage)
    private Optional<AuthorityPerson> internalFindById(final Long id) {
        log.debug("Finding authority person by ID:{}", id);
        final Optional<Optional<AuthorityPerson>> result = executeCommand(AuthorityPersonFacade.FIND_BY_ID, factory, Input.of(id));
        return result.flatMap(person -> {
            log.debug("Found authority person {}", person);
            return person.map(toPayload);
        });
    }

    // To create or update authority person instance (for entry-point)
    private Optional<AuthorityPerson> internalCreateOrUpdate(final Object... parameters) {
        return internalCreateOrUpdate(decodePersonArgument(parameters));
    }

    // To create or update authority person instance (for internal usage)
    private Optional<AuthorityPerson> internalCreateOrUpdate(final AuthorityPerson instance) {
        log.debug("Creating or Updating authority person {}", instance);
        final var input = Input.of(toPayload.apply(instance));
        final Optional<Optional<AuthorityPerson>> result = executeCommand(AuthorityPersonFacade.CREATE_OR_UPDATE, factory, input);
        return result.flatMap(person -> {
            log.debug("Changed authority person {}", person);
            return person.map(toPayload);
        });
    }

    // To create person instance + it's profile (for entry-point)
    private Optional<AuthorityPerson> internalCreateComposite(final Object... parameters) {
        return internalCreateComposite(decodePersonArgument(parameters));
    }

    // To create person instance + it's profile (for internal usage)
    private Optional<AuthorityPerson> internalCreateComposite(final AuthorityPerson instance) {
        log.debug("Creating authority person with new profile {}", instance);
        final var input = Input.of(toPayload.apply(instance));
        final Optional<Optional<AuthorityPerson>> result = executeCommand(AuthorityPersonFacade.CREATE_MACRO, factory, input);
        return result.flatMap( person ->{
            log.debug("Created authority person {}", person);
            return person.map(toPayload);
        });
    }


    // To delete authority person from the school (with profile at once) (for entry-point)
    private Void internalDeleteComposite(final Object... parameters) throws AuthorityPersonNotFoundException, AuthorityPersonManagesFacultyException {
        internalDeleteComposite(decodeLongArgument(parameters));
        return null;
    }

    // To delete authority person from the school (with profile at once) (for internal usage)
    private void internalDeleteComposite(final Long id) throws AuthorityPersonNotFoundException, AuthorityPersonManagesFacultyException {
        final String commandId = AuthorityPersonFacade.DELETE_MACRO;
        final Consumer<Exception> doOnError = exception -> {
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
        // deleting person and it's profile at once
        log.debug("Deleting authority person with ID:{} and it's profile", id);
        final Optional<Boolean> result = executeCommand(commandId, factory, Input.of(id), doOnError);
        result.ifPresent(executionResult ->
                log.debug("Deleted authority person with ID:{} successfully:{} .", id, executionResult)
        );
    }
}
