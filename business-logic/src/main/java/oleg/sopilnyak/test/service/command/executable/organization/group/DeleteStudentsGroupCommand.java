package oleg.sopilnyak.test.service.command.executable.organization.group;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentsGroupException;
import oleg.sopilnyak.test.school.common.exception.StudentGroupWithStudentsException;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.command.SchoolCommandCache;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Command-Implementation: command to delete the students group of the school by id
 *
 * @see StudentsGroup
 * @see StudentsGroupCommand
 * @see StudentsGroupPersistenceFacade
 * @see SchoolCommandCache
 */
@Slf4j
@Component
public class DeleteStudentsGroupCommand
        extends SchoolCommandCache<StudentsGroup>
        implements StudentsGroupCommand {
    private final StudentsGroupPersistenceFacade persistence;

    public DeleteStudentsGroupCommand(StudentsGroupPersistenceFacade persistence) {
        super(StudentsGroup.class);
        this.persistence = persistence;
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
     * @see StudentsGroupPersistenceFacade#toEntity(StudentsGroup)
     * @see StudentsGroupPersistenceFacade#deleteStudentsGroup(Long)
     * @see StudentsGroupPersistenceFacade#save(StudentsGroup)
     * @see NotExistStudentsGroupException
     */
    @Override
    public void executeDo(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to delete students group with ID: {}", parameter);
            final Long id = commandParameter(parameter);
            final var notFoundException = new NotExistStudentsGroupException(GROUP_WITH_ID_PREFIX + id + " is not exists.");
            if (PersistenceFacadeUtilities.isInvalidId(id)) {
                throw notFoundException;
            }
            final StudentsGroup entity =
                    retrieveEntity(id, persistence::findStudentsGroupById, persistence::toEntity, () -> notFoundException);

            if (!entity.getStudents().isEmpty()) {
                throw new StudentGroupWithStudentsException(GROUP_WITH_ID_PREFIX + id + " has students.");
            }

            // cached faculty is storing to context for further rollback (undo)
            context.setUndoParameter(entity);
            // deleting entity
            persistence.deleteStudentsGroup(id);
            context.setResult(true);
            log.debug("Deleted students group with ID: {} successfully.", id);
        } catch (Exception e) {
            rollbackCachedEntity(context, persistence::save);
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
    public void executeUndo(Context<?> context) {
        final Object parameter = context.getUndoParameter();
        try {
            log.debug("Trying to undo faculty deletion using: {}", parameter);

            rollbackCachedEntity(context, persistence::save);

            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo faculty deletion {}", parameter, e);
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
        return DELETE;
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
