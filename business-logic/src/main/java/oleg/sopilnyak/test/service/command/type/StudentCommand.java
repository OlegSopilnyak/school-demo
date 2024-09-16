package oleg.sopilnyak.test.service.command.type;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;

/**
 * Type for school-student command
 *
 * @see RootCommand
 * @see oleg.sopilnyak.test.school.common.model.Student
 */
public interface StudentCommand extends RootCommand {
    String STUDENT_WITH_ID_PREFIX = "Student with ID:";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "studentCommandsFactory";

    /**
     * ID of student's findById command
     */
    String FIND_BY_ID = "student.findById";
    /**
     * ID of student's findEnrolledTo command
     */
    String FIND_ENROLLED = "student.findEnrolledTo";
    /**
     * ID of student's findNotEnrolled command
     */
    String FIND_NOT_ENROLLED = "student.findNotEnrolled";
    /**
     * ID of student's createOrUpdate command
     */
    String CREATE_OR_UPDATE = "student.createOrUpdate";
    /**
     * ID of student's create command
     */
    String CREATE_NEW = "student.create.macro";
    /**
     * ID of student's delete command
     */
    String DELETE = "student.delete";
    /**
     * ID of student's delete macro-command
     */
    String DELETE_ALL = "student.delete.macro";

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
    default <T> Context<T> acceptPreparedContext(final PrepareContextVisitor visitor, final Object input) {
        return visitor.prepareContext(this, input);
    }

    /**
     * To execute command Do as a nested command
     *
     * @param visitor       visitor to do nested command execution
     * @param context       context for nested command execution
     * @param stateListener listener of context-state-change
     * @param <T>           type of command execution result
     * @see NestedCommandExecutionVisitor#doNestedCommand(RootCommand, Context, Context.StateChangedListener)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see StudentCommand#doCommand(Context)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     */
    @Override
    default <T> void doAsNestedCommand(final NestedCommandExecutionVisitor visitor,
                                       final Context<T> context,
                                       final Context.StateChangedListener<T> stateListener) {
        visitor.doNestedCommand(this, context, stateListener);
    }

    /**
     * To execute command Undo as a nested command
     *
     * @param visitor visitor to do nested command execution
     * @param context context for nested command execution
     * @param <T>     type of command execution result
     * @see NestedCommandExecutionVisitor#undoNestedCommand(RootCommand, Context)
     * @see StudentCommand#undoCommand(Context)
     */
    @Override
    default <T> Context<T> undoAsNestedCommand(final NestedCommandExecutionVisitor visitor, final Context<T> context) {
        return visitor.undoNestedCommand(this, context);
    }
}
