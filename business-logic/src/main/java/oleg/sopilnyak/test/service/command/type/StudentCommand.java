package oleg.sopilnyak.test.service.command.type;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.nested.TransferResultVisitor;
import org.slf4j.Logger;
import org.springframework.lang.NonNull;

/**
 * Type for school-student command
 *
 * @see SchoolCommand
 * @see oleg.sopilnyak.test.school.common.model.Student
 */
public interface StudentCommand extends SchoolCommand {
    String STUDENT_WITH_ID_PREFIX = "Student with ID:";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "studentCommandsFactory";

    /**
     * ID of student's findById command
     */
    String FIND_BY_ID_COMMAND_ID = "student.findById";
    /**
     * ID of student's findEnrolledTo command
     */
    String FIND_ENROLLED_COMMAND_ID = "student.findEnrolledTo";
    /**
     * ID of student's findNotEnrolled command
     */
    String FIND_NOT_ENROLLED_COMMAND_ID = "student.findNotEnrolled";
    /**
     * ID of student's createOrUpdate command
     */
    String CREATE_OR_UPDATE_COMMAND_ID = "student.createOrUpdate";
    /**
     * ID of student's delete command
     */
    String DELETE_COMMAND_ID = "student.delete";

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
     * @see PrepareContextVisitor#prepareContext(StudentCommand, Object)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Object)
     */
    @Override
    default <T> Context<T> acceptPreparedContext(@NonNull final PrepareContextVisitor visitor, final Object input) {
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
     * @see TransferResultVisitor#transferPreviousExecuteDoResult(StudentCommand, Object, Context)
     * @see Context#setRedoParameter(Object)
     */
    @Override
    default <S, T> void transferResultTo(@NonNull final TransferResultVisitor visitor,
                                         final S result, final Context<T> target) {
        visitor.transferPreviousExecuteDoResult(this, result, target);
    }

    /**
     * To execute command as a nested command
     *
     * @param visitor       visitor to do nested command execution
     * @param context       context for nested command execution
     * @param stateListener listener of context-state-change
     * @param <T>           type of command execution result
     * @see NestedCommandExecutionVisitor#doNestedCommand(SchoolCommand, Context, Context.StateChangedListener)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see StudentCommand#doCommand(Context)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     */
    @Override
    default <T> void doAsNestedCommand(@NonNull final NestedCommandExecutionVisitor visitor,
                                       final Context<T> context, final Context.StateChangedListener<T> stateListener) {
        visitor.doNestedCommand(this, context, stateListener);
    }
}
