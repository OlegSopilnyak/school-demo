package oleg.sopilnyak.test.service.command.type;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import oleg.sopilnyak.test.service.command.type.composite.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.composite.TransferResultVisitor;
import org.slf4j.Logger;

import java.util.Optional;

/**
 * Type for school-course command
 */
public interface CourseCommand extends SchoolCommand {
    String COURSE_WITH_ID_PREFIX = "Course with ID:";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "courseCommandsFactory";

    String FIND_BY_ID_COMMAND_ID = "course.findById";
    String FIND_REGISTERED_COMMAND_ID = "course.findRegisteredFor";
    String FIND_NOT_REGISTERED_COMMAND_ID = "course.findWithoutStudents";
    String CREATE_OR_UPDATE_COMMAND_ID = "course.createOrUpdate";
    String DELETE_COMMAND_ID = "course.delete";
    String REGISTER_COMMAND_ID = "course.register";
    String UN_REGISTER_COMMAND_ID = "course.unRegister";


    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    Logger getLog();

    /**
     * To execute command
     *
     * @param context context of redo execution
     * @see Context
     */
    @Override
    default <T> void doCommand(Context<T> context) {
        if (isWrongRedoStateOf(context)) {
            getLog().warn("Cannot do redo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        } else {
            // start redo with correct context state
            context.setState(Context.State.WORK);
            executeDo(context);
        }
    }

    /**
     * To rollback command's execution
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    @Override
    default <T> void undoCommand(Context<T> context) {
        if (isWrongUndoStateOf(context)) {
            getLog().warn("Cannot do undo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        } else {
            // start undo with correct context state
            context.setState(Context.State.WORK);
            executeUndo(context);
        }
    }

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor visitor of prepared contexts
     * @param input   Macro-Command call's input
     * @param <T>     type of command result
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(CourseCommand, Object)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Object)
     */
    @Override
    default <T> Context<T> acceptPreparedContext(PrepareContextVisitor visitor, Object input) {
        return visitor.prepareContext(this, input);
    }

    /**
     * To transfer command execution result to next command context
     *
     * @param visitor visitor for transfer result
     * @param result  result of command execution
     * @param target  command context for next execution
     * @param <S>     type of current command execution result
     * @param <T>     type of next command execution result
     * @see TransferResultVisitor#transferPreviousExecuteDoResult(CourseCommand, Optional, Context)
     * @see Context#setRedoParameter(Object)
     */
    @Override
    default <S, T> void transferResultTo(
            final TransferResultVisitor visitor, final Optional<S> result, final Context<T> target
    ) {
        visitor.transferPreviousExecuteDoResult(this, result, target);
    }
}
