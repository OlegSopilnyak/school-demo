package oleg.sopilnyak.test.service.facade.organization.impl;

import static oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand.CommandId;

import oleg.sopilnyak.test.school.common.business.facade.organization.StudentsGroupFacade;
import oleg.sopilnyak.test.school.common.business.facade.organization.base.OrganizationFacade;
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
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
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

    public StudentsGroupFacadeImpl(
            CommandsFactory<StudentsGroupCommand<?>> factory,
            BusinessMessagePayloadMapper mapper,
            CommandActionExecutor actionExecutor
    ) {
        super(factory, actionExecutor);
        this.toPayload = group -> group instanceof StudentsGroupPayload ? group : mapper.toPayload(group);
    }

    /**
     * To get all students groups
     *
     * @return list of students groups
     * @see StudentsGroup
     */
    @Override
    public Collection<StudentsGroup> findAllStudentsGroups() {
        log.debug("Finding all students groups");
        final Optional<Set<StudentsGroup>> result;
        result = executeCommand(CommandId.FIND_ALL, factory, Input.empty());
        if (result.isPresent()) {
            final Set<StudentsGroup> studentsGroupSet = result.get();
            log.debug("Found all students groups {}", studentsGroupSet);
            return studentsGroupSet.stream().map(toPayload).collect(Collectors.toSet());
        }
        return Set.of();
    }

    /**
     * To get the students group by ID
     *
     * @param id system-id of the students group
     * @return StudentsGroup instance or empty() if not exists
     * @see StudentsGroup
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<StudentsGroup> findStudentsGroupById(Long id) {
        log.debug("Finding students group by ID:{}", id);
        final Optional<Optional<StudentsGroup>> result;
        result = executeCommand(CommandId.FIND_BY_ID, factory, Input.of(id));
        if (result.isPresent()) {
            final Optional<StudentsGroup> faculty = result.get();
            log.debug("Found students group {}", faculty);
            return faculty.map(toPayload);
        }
        return Optional.empty();
    }

    /**
     * To create or update students group instance
     *
     * @param instance students group should be created or updated
     * @return students group instance or empty() if not exists
     * @see StudentsGroup
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<StudentsGroup> createOrUpdateStudentsGroup(StudentsGroup instance) {
        log.debug("Creating or Updating students group {}", instance);
        final var input = Input.of(toPayload.apply(instance));
        final Optional<Optional<StudentsGroup>> result;
        result = executeCommand(CommandId.CREATE_OR_UPDATE, factory, input);
        if (result.isPresent()) {
            final Optional<StudentsGroup> studentsGroup = result.get();
            log.debug("Changed students group {}", studentsGroup);
            return studentsGroup.map(toPayload);
        }
        return Optional.empty();
    }

    /**
     * To delete students group instance from the school
     *
     * @param id system-id of the students group to delete
     * @throws StudentsGroupNotFoundException    throws when students group is not exists
     * @throws StudentGroupWithStudentsException throws when students group has students
     */
    @Override
    public void deleteStudentsGroupById(Long id) throws StudentsGroupNotFoundException, StudentGroupWithStudentsException {
        final String commandId = CommandId.DELETE;
        final Consumer<Exception> doThisOnError = exception -> {
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
        final Optional<Boolean> result = executeCommand(commandId, factory, Input.of(id), doThisOnError);
        result.ifPresent(executionResult ->
                log.debug("Deleted faculty with ID:{} successfully:{} .", id, executionResult)
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
