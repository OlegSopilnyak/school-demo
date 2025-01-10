package oleg.sopilnyak.test.service.command.type.profile;

import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;

/**
 * Type for school-student-profile commands
 */
public interface StudentProfileCommand<T> extends ProfileCommand<T> {
    /**
     * ID of findById student profile command
     */
    String FIND_BY_ID = "profile.student.findById";
    /**
     * ID of deleteById student profile command
     */
    String DELETE_BY_ID = "profile.student.deleteById";
    /**
     * ID of createOrUpdate student profile command
     */
    String CREATE_OR_UPDATE = "profile.student.createOrUpdate";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "studentProfileCommandsFactory";

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(StudentProfileCommand, Input)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Input)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareContextVisitor visitor, final Input<?> macroInputParameter) {
        return visitor.prepareContext(this, macroInputParameter);
    }

    /**
     * To execute command Do as a nested command
     *
     * @param visitor       visitor to do nested command execution
     * @param context       context for nested command execution
     * @param stateListener listener of context-state-change
     * @see NestedCommandExecutionVisitor#doNestedCommand(RootCommand, Context, Context.StateChangedListener)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see StudentProfileCommand#doCommand(Context)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     */
    @Override
    default void doAsNestedCommand(final NestedCommandExecutionVisitor visitor,
                                   final Context<?> context, final Context.StateChangedListener stateListener) {
        visitor.doNestedCommand(this, (Context<T>) context, stateListener);
    }

    /**
     * To execute command Undo as a nested command
     *
     * @param visitor visitor to do nested command execution
     * @param context context for nested command execution
     * @see NestedCommandExecutionVisitor#undoNestedCommand(RootCommand, Context)
     * @see StudentProfileCommand#undoCommand(Context)
     */
    @Override
    default Context<?> undoAsNestedCommand(final NestedCommandExecutionVisitor visitor, final Context<?> context) {
        return visitor.undoNestedCommand(this, context);
    }
}
