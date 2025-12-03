package oleg.sopilnyak.test.service.facade.organization.impl;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.doSimpleCommand;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.takeValidCommand;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.throwFor;
import static oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand.CommandId;

import oleg.sopilnyak.test.school.common.business.facade.organization.StudentsGroupFacade;
import oleg.sopilnyak.test.school.common.business.facade.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.exception.organization.StudentGroupWithStudentsException;
import oleg.sopilnyak.test.school.common.exception.organization.StudentsGroupNotFoundException;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.facade.organization.base.impl.OrganizationFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentsGroupPayload;

import java.util.Collection;
import java.util.Optional;
import java.util.function.UnaryOperator;
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
            ActionExecutor actionExecutor
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
        log.debug("Find all students groups");
        final Collection<StudentsGroup> result = doSimpleCommand(CommandId.FIND_ALL, null, factory);
        log.debug("Found all students groups {}", result);
        return result.stream().map(toPayload).toList();
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
        log.debug("Find students group by ID:{}", id);
        final Optional<StudentsGroup> result = doSimpleCommand(CommandId.FIND_BY_ID, Input.of(id), factory);
        log.debug("Found students group {}", result);
        return result.map(toPayload);
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
        log.debug("Create or Update students group {}", instance);
        final Optional<StudentsGroup> result = doSimpleCommand(CommandId.CREATE_OR_UPDATE, Input.of(toPayload.apply(instance)), factory);
        log.debug("Changed students group {}", result);
        return result.map(toPayload);
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
        log.debug("Delete students group with ID:{}", id);
        final String commandId = CommandId.DELETE;
        final RootCommand<Boolean> command = (RootCommand<Boolean>) takeValidCommand(commandId, factory);
        final Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        if (context.isDone()) {
            // success processing
            log.debug("Deleted authority person with ID:{} successfully.", id);
            return;
        }

        // fail processing
        final Exception doException = context.getException();
        log.warn(SOMETHING_WENT_WRONG, doException);
        if (doException instanceof StudentsGroupNotFoundException noGroupException) {
            throw noGroupException;
        } else if (doException instanceof StudentGroupWithStudentsException exception) {
            throw exception;
        } else if (nonNull(doException)) {
            throwFor(commandId, doException);
        } else {
            wrongCommandExecution();
        }
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
    private static void wrongCommandExecution() {
        log.error(WRONG_COMMAND_EXECUTION, CommandId.DELETE);
        throwFor(CommandId.DELETE, new NullPointerException(EXCEPTION_IS_NOT_STORED));
    }
}
