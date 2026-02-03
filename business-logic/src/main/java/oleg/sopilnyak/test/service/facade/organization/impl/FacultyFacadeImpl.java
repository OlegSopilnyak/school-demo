package oleg.sopilnyak.test.service.facade.organization.impl;

import oleg.sopilnyak.test.school.common.business.facade.organization.FacultyFacade;
import oleg.sopilnyak.test.school.common.business.facade.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.organization.FacultyIsNotEmptyException;
import oleg.sopilnyak.test.school.common.exception.organization.FacultyNotFoundException;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.facade.organization.base.impl.OrganizationFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.FacultyPayload;

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
 * Service-Facade: Service for manage organization in the school (groups of courses)
 *
 * @see OrganizationFacade
 * @see OrganizationFacadeImpl
 * @see Faculty
 * @see FacultyCommand
 */
@Slf4j
public class FacultyFacadeImpl extends OrganizationFacadeImpl<FacultyCommand<?>> implements FacultyFacade {
    // semantic data to payload converter
    private final UnaryOperator<Faculty> toPayload;
    //
    // setting up action-methods by action-id
    private final Map<String, Function<Object[], Object>> actions = Map.<String, Function<Object[], Object>>of(
            FacultyFacade.FIND_ALL, this::internalFindAll,
            FacultyFacade.FIND_BY_ID, this::internalFindById,
            FacultyFacade.CREATE_OR_UPDATE, this::internalCreateOrUpdate,
            FacultyFacade.DELETE, this::internalDeleteById
    ).entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey, Map.Entry::getValue,
                    (existing, _) -> existing,
                    HashMap::new
            )
    );

    public FacultyFacadeImpl(
            CommandsFactory<FacultyCommand<?>> factory,
            BusinessMessagePayloadMapper mapper,
            CommandActionExecutor actionExecutor
    ) {
        super(factory, actionExecutor);
        this.toPayload = faculty -> faculty instanceof FacultyPayload ? faculty : mapper.toPayload(faculty);
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
     * To get the logger of the facade
     *
     * @return logger instance
     */
    @Override
    public Logger getLogger() {
        return log;
    }

    // private methods
    // to decode first faculty from parameters array
    private static Faculty decodeFacultyArgument(final Object... parameters) {
        if (parameters == null || parameters.length < 1) {
            throw new IllegalArgumentException("Wrong number of parameters");
        }
        if (parameters[0] instanceof Faculty value) {
            return value;
        } else {
            throw new InvalidParameterTypeException("Faculty", parameters[0]);
        }
    }

    // to get the faculty by ID (for entry-point)
    private Optional<Faculty> internalFindById(final Object... parameters) {
        return internalFindById(decodeLongArgument(parameters));
    }

    // to get the faculty by ID (for internal usage)
    private Optional<Faculty> internalFindById(final Long id) {
        log.debug("Finding faculty by ID:{}", id);
        final Optional<Optional<Faculty>> result = executeCommand(FacultyFacade.FIND_BY_ID, factory, Input.of(id));
        return result.flatMap(faculty -> {
            log.debug("Found faculty {}", faculty);
            return faculty.map(toPayload);
        });
    }

    // to get all faculties (for entry-point)
    private Collection<Faculty> internalFindAll(final Object... parameters) {
        return internalFindAll();
    }

    // to get all faculties (for internal usage)
    private Collection<Faculty> internalFindAll() {
        log.debug("Finding all faculties");
        final Optional<Set<Faculty>> result = executeCommand(FacultyFacade.FIND_ALL, factory, Input.empty());
        return result.map(entities -> {
            log.debug("Found faculties {}", entities);
            return entities.stream().map(toPayload).collect(Collectors.toSet());
        }).orElseGet(Set::of);
    }

    // to create or update faculty instance (for entry-point)
    private Optional<Faculty> internalCreateOrUpdate(final Object... parameters) {
        return internalCreateOrUpdate(decodeFacultyArgument(parameters));
    }

    // to create or update faculty instance (for internal usage)
    private Optional<Faculty> internalCreateOrUpdate(final Faculty instance) {
        log.debug("Creating or Updating faculty {}", instance);
        final var input = Input.of(toPayload.apply(instance));
        final Optional<Optional<Faculty>> result = executeCommand(FacultyFacade.CREATE_OR_UPDATE, factory, input);
        return result.flatMap(faculty -> {
            log.debug("Changed faculty {}", faculty);
            return faculty.map(toPayload);
        });
    }

    // to delete the faculty by ID (for entry-point)
    private Void internalDeleteById(final Object... parameters) {
        internalDeleteById(decodeLongArgument(parameters));
        return null;
    }

    // to delete the faculty by ID (for internal usage)
    private void internalDeleteById(final Long id) {
        final String commandId = FacultyFacade.DELETE;
        // setting up customized errors handler
        final Consumer<Exception> doOnError = exception -> {
            switch (exception) {
                case FacultyNotFoundException noFacultyException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw noFacultyException;
                }
                case FacultyIsNotEmptyException notEmptyException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw notEmptyException;
                }
                case null, default -> defaultDoOnError(commandId).accept(exception);
            }
        };

        log.debug("Deleting faculty with ID:{}", id);
        final Optional<Boolean> result = executeCommand(commandId, factory, Input.of(id), doOnError);
        result.ifPresent(executionResult ->
                log.debug("Deleted faculty with ID:{} successfully:{} .", id, executionResult)
        );
    }
}
