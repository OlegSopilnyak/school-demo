package oleg.sopilnyak.test.service.command.executable.organization.group;

import oleg.sopilnyak.test.school.common.exception.organization.StudentsGroupNotFoundException;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.core.cache.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.executable.core.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Optional;
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
 * Command-Implementation: command to update the students group of the school
 *
 * @see StudentsGroup
 * @see StudentsGroupCommand
 * @see StudentsGroupPersistenceFacade
 * @see SchoolCommandCache
 */
@Slf4j
@Component(StudentsGroupCommand.Component.CREATE_OR_UPDATE)
public class CreateOrUpdateStudentsGroupCommand extends SchoolCommandCache<StudentsGroup, Optional<StudentsGroup>>
        implements StudentsGroupCommand<Optional<StudentsGroup>> {
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
        return StudentsGroupCommand.Component.CREATE_OR_UPDATE;
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return CommandId.CREATE_OR_UPDATE;
    }

    public CreateOrUpdateStudentsGroupCommand(final StudentsGroupPersistenceFacade persistence,
                                              final BusinessMessagePayloadMapper payloadMapper) {
        super(StudentsGroup.class);
        this.persistence = persistence;
        this.payloadMapper = payloadMapper;
    }

    /**
     * DO: To create or update students group instance<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     * @see SchoolCommandCache#retrieveEntity(Long, LongFunction, UnaryOperator, Supplier)
     * @see SchoolCommandCache#persistRedoEntity(Context, Function)
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     * @see SchoolCommandCache#restoreInitialCommandState(Context, Function)
     * @see StudentsGroupPersistenceFacade#findStudentsGroupById(Long)
     * @see StudentsGroupPersistenceFacade#save(StudentsGroup)
     * @see StudentsGroupNotFoundException
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeDo(Context<Optional<StudentsGroup>> context) {
        final Input<StudentsGroup> parameter = context.getRedoParameter();
        try {
            checkNullParameter(parameter);
            final Long id = parameter.value().getId();
            log.debug("Trying to create or update students group {}", id);
            final boolean isCreateEntityMode = PersistenceFacadeUtilities.isInvalidId(id);
            if (!isCreateEntityMode) {
                // cached students group is storing to context for further rollback (undo)
                final StudentsGroup entity = retrieveEntity(
                        id, persistence::findStudentsGroupById, this::adoptEntity,
                        () -> new StudentsGroupNotFoundException(GROUP_WITH_ID_PREFIX + id + " is not exists.")
                );

                log.debug("Previous value of the entity stored for possible command's undo: {}", entity);
                if (context instanceof CommandContext<?> commandContext) {
                    commandContext.setUndoParameter(Input.of(entity));
                }
            } else {
                log.debug("Trying to create students group using: {}", parameter);
            }
            // persisting entity trough persistence layer
            final Optional<StudentsGroup> persisted = persistRedoEntity(context, persistence::save);
            // checking command context state after entity persistence
            afterEntityPersistenceCheck(
                    context, () -> rollbackCachedEntity(context, persistence::save),
                    persisted.map(this::adoptEntity).orElse(null), isCreateEntityMode
            );
        } catch (Exception e) {
            log.error("Cannot create or students group faculty '{}'", parameter, e);
            context.failed(e);
            restoreInitialCommandState(context, persistence::save);
        }
    }

    /**
     * UNDO: To create or update students group instance<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context.State#UNDONE
     * @see Context#getUndoParameter()
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     * @see StudentsGroupPersistenceFacade#save(StudentsGroup)
     * @see StudentsGroupPersistenceFacade#deleteStudentsGroup(Long)
     * @see StudentsGroupNotFoundException
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void executeUndo(Context<?> context) {
        final Input<StudentsGroup> parameter = context.getUndoParameter();
        try {
            checkNullParameter(parameter);
            log.debug("Trying to undo students group changes using: {}", parameter);

            rollbackCachedEntity(context, persistence::save, persistence::deleteStudentsGroup);

            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo students group change {}", parameter, e);
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
}
