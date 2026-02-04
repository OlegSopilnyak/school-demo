package oleg.sopilnyak.test.service.facade.organization.impl;

import oleg.sopilnyak.test.school.common.business.facade.organization.StudentsGroupFacade;
import oleg.sopilnyak.test.school.common.business.facade.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.organization.StudentGroupWithStudentsException;
import oleg.sopilnyak.test.school.common.exception.organization.StudentsGroupNotFoundException;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.facade.organization.base.impl.OrganizationFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentsGroupPayload;

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
 * Service-Facade: Service for manage organization in the school (school students groups)
 *
 * @see OrganizationFacade
 * @see StudentsGroup
 */
@Slf4j
public class StudentsGroupFacadeImpl extends OrganizationFacadeImpl<StudentsGroupCommand<?>> implements StudentsGroupFacade {
    // semantic data to payload converter
    private final UnaryOperator<StudentsGroup> toPayload;
    //
    // setting up action-methods by action-id
    private final Map<String, Function<Object[], Object>> actions = Map.<String, Function<Object[], Object>>of(
            StudentsGroupFacade.FIND_ALL, this::internalFindAll,
            StudentsGroupFacade.FIND_BY_ID, this::internalFindById,
            StudentsGroupFacade.CREATE_OR_UPDATE, this::internalCreateOrUpdate,
            StudentsGroupFacade.DELETE, this::internalDeleteById
    ).entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey, Map.Entry::getValue,
                    (existing, _) -> existing,
                    HashMap::new
            )
    );

    public StudentsGroupFacadeImpl(
            CommandsFactory<StudentsGroupCommand<?>> factory,
            BusinessMessagePayloadMapper mapper,
            CommandActionExecutor actionExecutor
    ) {
        super(factory, actionExecutor);
        this.toPayload = group -> group instanceof StudentsGroupPayload ? group : mapper.toPayload(group);
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
    // to decode first students group from parameters array
    private StudentsGroup decodeStudentsGroupArgument(Object[] parameters) {
        if (parameters == null || parameters.length < 1) {
            throw new IllegalArgumentException("Wrong number of parameters");
        }
        if (parameters[0] instanceof StudentsGroup value) {
            return value;
        } else {
            throw new InvalidParameterTypeException("StudentsGroup", parameters[0]);
        }
    }
    // to get the students group by ID (for entry-point)
    private Optional<StudentsGroup> internalFindById(final Object... parameters) {
        return internalFindById(decodeLongArgument(parameters));
    }

    // to get the students group by ID (for internal usage)
    private Optional<StudentsGroup> internalFindById(final Long id) {
        log.debug("Finding students group by ID:{}", id);
        final Optional<Optional<StudentsGroup>> result = executeCommand(StudentsGroupFacade.FIND_BY_ID, factory, Input.of(id));
        return result.flatMap(studentsGroup -> {
            log.debug("Found students group {}", studentsGroup);
            return studentsGroup.map(toPayload);
        });
    }

    // to get all students groups (for entry-point)
    private Collection<StudentsGroup> internalFindAll(final Object... parameters) {
        return internalFindAll();
    }

    // to get all students groups (for internal usage)
    private Collection<StudentsGroup> internalFindAll() {
        log.debug("Finding all students groups");
        final Optional<Set<StudentsGroup>> result = executeCommand(StudentsGroupFacade.FIND_ALL, factory, Input.empty());
        return result.map(entities -> {
            log.debug("Found all students groups {}", entities);
            return entities.stream().map(toPayload).collect(Collectors.toSet());
        }).orElseGet(Set::of);
    }

    // to create or update students group instance (for entry-point)
    private Optional<StudentsGroup> internalCreateOrUpdate(final Object... parameters) {
        return internalCreateOrUpdate(decodeStudentsGroupArgument(parameters));
    }

    // to create or update students group instance (for internal usage)
    private Optional<StudentsGroup> internalCreateOrUpdate(final StudentsGroup instance) {
        log.debug("Creating or Updating students group {}", instance);
        final var input = Input.of(toPayload.apply(instance));
        final Optional<Optional<StudentsGroup>> result = executeCommand(StudentsGroupFacade.CREATE_OR_UPDATE, factory, input);
        return result.flatMap(studentsGroup -> {
            log.debug("Changed students group {}", studentsGroup);
            return studentsGroup.map(toPayload);
        });
    }

    // to delete the students group by ID (for entry-point)
    private Void internalDeleteById(final Object... parameters) {
        internalDeleteById(decodeLongArgument(parameters));
        return null;
    }

    // to delete the students group by ID (for internal usage)
    private void internalDeleteById(final Long id) {
        final String commandId = StudentsGroupFacade.DELETE;
        // setting up customized errors handler
        final Consumer<Exception> doOnError = exception -> {
            switch (exception) {
                case StudentsGroupNotFoundException noGroupException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw noGroupException;
                }
                case StudentGroupWithStudentsException groupWithStudentsException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw groupWithStudentsException;
                }
                case null, default -> defaultDoOnError(commandId).accept(exception);
            }
        };

        log.debug("Deleting students group with ID:{}", id);
        final Optional<Boolean> result = executeCommand(commandId, factory, Input.of(id), doOnError);
        result.ifPresent(executionResult ->
                log.debug("Deleted students group with ID:{} successfully:{} .", id, executionResult)
        );
    }
}
