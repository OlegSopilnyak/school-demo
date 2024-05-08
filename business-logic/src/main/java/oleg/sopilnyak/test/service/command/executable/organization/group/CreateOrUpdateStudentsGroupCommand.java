package oleg.sopilnyak.test.service.command.executable.organization.group;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentsGroupException;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.command.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Command-Implementation: command to update the students group of the school
 *
 * @see StudentsGroup
 * @see StudentsGroupCommand
 * @see StudentsGroupPersistenceFacade
 * @see SchoolCommandCache
 */
@Slf4j
@Component
public class CreateOrUpdateStudentsGroupCommand
        extends SchoolCommandCache<StudentsGroup>
        implements StudentsGroupCommand {
    private final StudentsGroupPersistenceFacade persistence;

    public CreateOrUpdateStudentsGroupCommand(StudentsGroupPersistenceFacade persistence) {
        super(StudentsGroup.class);
        this.persistence = persistence;
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
     * @see StudentsGroupPersistenceFacade#findStudentsGroupById(Long)
     * @see StudentsGroupPersistenceFacade#toEntity(StudentsGroup)
     * @see StudentsGroupPersistenceFacade#save(StudentsGroup)
     * @see NotExistStudentsGroupException
     */
    @Override
    public <T> void executeDo(Context<T> context) {
        final Object parameter = context.getRedoParameter();
        try {
            check(parameter);
            log.debug("Trying to create or update students group {}", parameter);
            final Long id = ((StudentsGroup) parameter).getId();
            final boolean isCreateEntity = PersistenceFacadeUtilities.isInvalidId(id);
            if (!isCreateEntity) {
                // cached students group is storing to context for further rollback (undo)
                context.setUndoParameter(
                        retrieveEntity(id, persistence::findStudentsGroupById, persistence::toEntity,
                                () -> new NotExistStudentsGroupException(GROUP_WITH_ID_PREFIX + id + " is not exists.")
                        )
                );
            }

            final Optional<StudentsGroup> persisted = persistRedoEntity(context, persistence::save);
            // checking execution context state
            afterPersistCheck(context, () -> rollbackCachedEntity(context, persistence::save), persisted, isCreateEntity);
        } catch (Exception e) {
            log.error("Cannot create or students group faculty '{}'", parameter, e);
            context.failed(e);
            rollbackCachedEntity(context, persistence::save);
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
     * @see NotExistStudentsGroupException
     */
    @Override
    public <T> void executeUndo(Context<T> context) {
        final Object parameter = context.getUndoParameter();
        try {
            log.debug("Trying to undo students group changes using: {}", parameter);

            rollbackCachedEntity(context, persistence::save, persistence::deleteStudentsGroup,
                    () -> new NotExistStudentsGroupException("Wrong undo parameter :" + parameter));

            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo students group change {}", parameter, e);
            context.failed(e);
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return CREATE_OR_UPDATE;
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
