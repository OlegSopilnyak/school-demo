package oleg.sopilnyak.test.service.command.executable.organization.group;

import oleg.sopilnyak.test.school.common.exception.EntityNotFoundException;
import oleg.sopilnyak.test.school.common.exception.organization.StudentGroupWithStudentsException;
import oleg.sopilnyak.test.school.common.exception.organization.StudentsGroupNotFoundException;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.core.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.executable.core.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Command-Implementation: command to delete the students group of the school by id
 *
 * @see StudentsGroup
 * @see StudentsGroupCommand
 * @see StudentsGroupPersistenceFacade
 * @see SchoolCommandCache
 */
@Slf4j
@Component(StudentsGroupCommand.Component.DELETE)
public class DeleteStudentsGroupCommand extends SchoolCommandCache<StudentsGroup, Boolean>
        implements StudentsGroupCommand<Boolean> {
    private final transient StudentsGroupPersistenceFacade persistence;
    @Getter
    private final transient BusinessMessagePayloadMapper payloadMapper;

    /**
     * The name of command bean in spring beans factory
     *
     * @return spring name of the command
     */
    @Override
    public String springName() {
        return StudentsGroupCommand.Component.DELETE;
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return CommandId.DELETE;
    }

    public DeleteStudentsGroupCommand(final StudentsGroupPersistenceFacade persistence,
                                      final BusinessMessagePayloadMapper payloadMapper) {
        super(StudentsGroup.class);
        this.persistence = persistence;
        this.payloadMapper = payloadMapper;
    }

    /**
     * DO: To delete students group by id<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     * @see SchoolCommandCache#retrieveEntity(Long, LongFunction, UnaryOperator, Supplier)
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     * @see StudentsGroupPersistenceFacade#findStudentsGroupById(Long)
     * @see StudentsGroupPersistenceFacade#deleteStudentsGroup(Long)
     * @see StudentsGroupPersistenceFacade#save(StudentsGroup)
     * @see StudentsGroupNotFoundException
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeDo(Context<Boolean> context) {
        final Input<Long> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            final Long id = parameter.value();
            log.debug("Trying to delete students group with ID: {}", id);
            if (PersistenceFacadeUtilities.isInvalidId(id)) {
                log.warn("Invalid id {}", id);
                throw exceptionFor(id);
            }
            final StudentsGroup entity = retrieveEntity(
                    id, persistence::findStudentsGroupById, this::adoptEntity, () -> exceptionFor(id)
            );
            if (!entity.getStudents().isEmpty()) {
                log.warn(GROUP_WITH_ID_PREFIX + "{} has students.", id);
                throw new StudentGroupWithStudentsException(GROUP_WITH_ID_PREFIX + id + " has students.");
            }
            // removing students group instance by ID from the database
            persistence.deleteStudentsGroup(id);
            // setup undo parameter for deleted entity
            prepareDeleteEntityUndo(context, entity, () -> exceptionFor(id));
            // successful delete entity operation
            context.setResult(true);
            log.debug("Deleted students group with ID: {} successfully.", id);
        } catch (Exception e) {
            log.error("Cannot delete students group with :{}", parameter, e);
            context.failed(e);
        }
    }

    /**
     * UNDO: To delete students group by id<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     * @see Context.State#UNDONE
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     * @see StudentsGroupPersistenceFacade#save(StudentsGroup)
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeUndo(Context<?> context) {
        final Input<?> parameter = context.getUndoParameter();
        try {
            checkNullParameter(parameter);
            log.debug("Trying to undo students group deletion using: {}", parameter);

            final StudentsGroup entity = rollbackCachedEntity(context, persistence::save).orElseThrow();

            log.debug("Updated in database: '{}'", entity);
            // change students-group-id value for further do command doingMainLoop
            if (context instanceof CommandContext<?> commandContext) {
                commandContext.setRedoParameter(Input.of(entity.getId()));
            }
            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo students group deletion {}", parameter, e);
            context.failed(e);
        }
    }

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    @Override
    public Logger getLog() {
        return log;
    }

    // private methods
    private EntityNotFoundException exceptionFor(final Long id) {
        return new StudentsGroupNotFoundException(GROUP_WITH_ID_PREFIX + id + " is not exists.");
    }
}
