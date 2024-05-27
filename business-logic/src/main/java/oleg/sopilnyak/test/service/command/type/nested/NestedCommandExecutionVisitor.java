package oleg.sopilnyak.test.service.command.type.nested;

import oleg.sopilnyak.test.service.command.type.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.NestedCommand;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;
import org.slf4j.Logger;

/**
 * Visitor: Execute nested command
 */
public interface NestedCommandExecutionVisitor {
    /**
     * To execute nested command
     *
     * @param command       command to execute
     * @param doContext     context for execution
     * @param stateListener the lister of command state change
     * @param <T>           type of command execution result
     * @see SchoolCommand#doCommand(Context)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     * @see Context.State#READY
     * @see Context.State#DONE
     * @see Context.State#FAIL
     */
    default <T> void doNestedCommand(final SchoolCommand command,
                                     final Context<T> doContext,
                                     final Context.StateChangedListener<T> stateListener) {
        defaultDoNestedCommand(command, doContext, stateListener);
    }

    /**
     * To execute nested command
     *
     * @param command       command to execute
     * @param doContext     context for execution
     * @param stateListener the lister of command state change
     * @param <T>           type of command execution result
     * @see StudentCommand#doCommand(Context)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     * @see Context.State#READY
     * @see Context.State#DONE
     * @see Context.State#FAIL
     */
    default <T> void doNestedCommand(final StudentCommand command,
                                     final Context<T> doContext,
                                     final Context.StateChangedListener<T> stateListener) {
        defaultDoNestedCommand(command, doContext, stateListener);
    }

    /**
     * To execute nested command
     *
     * @param command       command to execute
     * @param doContext     context for execution
     * @param stateListener the lister of command state change
     * @param <T>           type of command execution result
     * @see CourseCommand#doCommand(Context)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     * @see Context.State#READY
     * @see Context.State#DONE
     * @see Context.State#FAIL
     */
    default <T> void doNestedCommand(final CourseCommand command,
                                     final Context<T> doContext,
                                     final Context.StateChangedListener<T> stateListener) {
        defaultDoNestedCommand(command, doContext, stateListener);
    }

    /**
     * To execute nested command
     *
     * @param command       command to execute
     * @param doContext     context for execution
     * @param stateListener the lister of command state change
     * @param <T>           type of command execution result
     * @see CompositeCommand#doCommand(Context)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     * @see Context.State#READY
     * @see Context.State#DONE
     * @see Context.State#FAIL
     */
    default <T, C extends SchoolCommand> void doNestedCommand(final CompositeCommand<C> command,
                                                              final Context<T> doContext,
                                                              final Context.StateChangedListener<T> stateListener) {
        defaultDoNestedCommand(command, doContext, stateListener);
    }

    /**
     * To execute nested command
     *
     * @param command       command to execute
     * @param doContext     context for execution
     * @param stateListener the lister of command state change
     * @param <T>           type of command execution result
     * @see StudentProfileCommand#doCommand(Context)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     * @see Context.State#READY
     * @see Context.State#DONE
     * @see Context.State#FAIL
     */
    default <T> void doNestedCommand(final StudentProfileCommand command,
                                     final Context<T> doContext,
                                     final Context.StateChangedListener<T> stateListener) {
        defaultDoNestedCommand(command, doContext, stateListener);
    }

    /**
     * To execute nested command
     *
     * @param command       command to execute
     * @param doContext     context for execution
     * @param stateListener the lister of command state change
     * @param <T>           type of command execution result
     * @see PrincipalProfileCommand#doCommand(Context)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     * @see Context.State#READY
     * @see Context.State#DONE
     * @see Context.State#FAIL
     */
    default <T> void doNestedCommand(final PrincipalProfileCommand command,
                                     final Context<T> doContext,
                                     final Context.StateChangedListener<T> stateListener) {
        defaultDoNestedCommand(command, doContext, stateListener);
    }

    /**
     * To execute nested command
     *
     * @param command       command to execute
     * @param doContext     context for execution
     * @param stateListener the lister of command state change
     * @param <T>           type of command execution result
     * @see StudentsGroupCommand#doCommand(Context)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     * @see Context.State#READY
     * @see Context.State#DONE
     * @see Context.State#FAIL
     */
    default <T> void doNestedCommand(final StudentsGroupCommand command,
                                     final Context<T> doContext,
                                     final Context.StateChangedListener<T> stateListener) {
        defaultDoNestedCommand(command, doContext, stateListener);
    }

    /**
     * To execute nested command
     *
     * @param command       command to execute
     * @param doContext     context for execution
     * @param stateListener the lister of command state change
     * @param <T>           type of command execution result
     * @see FacultyCommand#doCommand(Context)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     * @see Context.State#READY
     * @see Context.State#DONE
     * @see Context.State#FAIL
     */
    default <T> void doNestedCommand(final FacultyCommand command,
                                     final Context<T> doContext,
                                     final Context.StateChangedListener<T> stateListener) {
        defaultDoNestedCommand(command, doContext, stateListener);
    }

    /**
     * To execute nested command
     *
     * @param command       command to execute
     * @param doContext     context for execution
     * @param stateListener the lister of command state change
     * @param <T>           type of command execution result
     * @see AuthorityPersonCommand#doCommand(Context)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     * @see Context.State#READY
     * @see Context.State#DONE
     * @see Context.State#FAIL
     */
    default <T> void doNestedCommand(final AuthorityPersonCommand command,
                                     final Context<T> doContext,
                                     final Context.StateChangedListener<T> stateListener) {
        defaultDoNestedCommand(command, doContext, stateListener);
    }

    /**
     * To rollback changes for contexts with state DONE
     *
     * @param command     nested command to do undo with nested context (could be Override)
     * @param undoContext nested context with DONE state
     * @see SchoolCommand#undoCommand(Context)
     * @see Context.State#DONE
     * @see Context.State#UNDONE
     * @see Context.State#FAIL
     */
    default <T> Context<T> undoNestedCommand(final SchoolCommand command, final Context<T> undoContext) {
        return defaultUndoNestedCommand(command, undoContext);
    }

    /**
     * To rollback changes for contexts with state DONE
     *
     * @param command     nested command to do undo with nested context (could be Override)
     * @param undoContext nested context with DONE state
     * @see StudentCommand#undoCommand(Context)
     * @see Context.State#DONE
     * @see Context.State#UNDONE
     * @see Context.State#FAIL
     */
    default <T> Context<T> undoNestedCommand(final StudentCommand command, final Context<T> undoContext) {
        return defaultUndoNestedCommand(command, undoContext);
    }

    /**
     * To rollback changes for contexts with state DONE
     *
     * @param command     nested command to do undo with nested context (could be Override)
     * @param undoContext nested context with DONE state
     * @see CourseCommand#undoCommand(Context)
     * @see Context.State#DONE
     * @see Context.State#UNDONE
     * @see Context.State#FAIL
     */
    default <T> Context<T> undoNestedCommand(final CourseCommand command, final Context<T> undoContext) {
        return defaultUndoNestedCommand(command, undoContext);
    }

    /**
     * To rollback changes for contexts with state DONE
     *
     * @param command     nested command to do undo with nested context (could be Override)
     * @param undoContext nested context with DONE state
     * @see CompositeCommand#undoCommand(Context)
     * @see Context.State#DONE
     * @see Context.State#UNDONE
     * @see Context.State#FAIL
     */
    default <T> Context<T> undoNestedCommand(final CompositeCommand<SchoolCommand> command,
                                             final Context<T> undoContext) {
        return defaultUndoNestedCommand(command, undoContext);
    }

    /**
     * To rollback changes for contexts with state DONE
     *
     * @param command     nested command to do undo with nested context (could be Override)
     * @param undoContext nested context with DONE state
     * @see StudentProfileCommand#undoCommand(Context)
     * @see Context.State#DONE
     * @see Context.State#UNDONE
     * @see Context.State#FAIL
     */
    default <T> Context<T> undoNestedCommand(final StudentProfileCommand command, final Context<T> undoContext) {
        return defaultUndoNestedCommand(command, undoContext);
    }

    /**
     * To rollback changes for contexts with state DONE
     *
     * @param command     nested command to do undo with nested context (could be Override)
     * @param undoContext nested context with DONE state
     * @see PrincipalProfileCommand#undoCommand(Context)
     * @see Context.State#DONE
     * @see Context.State#UNDONE
     * @see Context.State#FAIL
     */
    default <T> Context<T> undoNestedCommand(final PrincipalProfileCommand command, final Context<T> undoContext) {
        return defaultUndoNestedCommand(command, undoContext);
    }

    /**
     * To rollback changes for contexts with state DONE
     *
     * @param command     nested command to do undo with nested context (could be Override)
     * @param undoContext nested context with DONE state
     * @see StudentsGroupCommand#undoCommand(Context)
     * @see Context.State#DONE
     * @see Context.State#UNDONE
     * @see Context.State#FAIL
     */
    default <T> Context<T> undoNestedCommand(final StudentsGroupCommand command, final Context<T> undoContext) {
        return defaultUndoNestedCommand(command, undoContext);
    }

    /**
     * To rollback changes for contexts with state DONE
     *
     * @param command     nested command to do undo with nested context (could be Override)
     * @param undoContext nested context with DONE state
     * @see FacultyCommand#undoCommand(Context)
     * @see Context.State#DONE
     * @see Context.State#UNDONE
     * @see Context.State#FAIL
     */
    default <T> Context<T> undoNestedCommand(final FacultyCommand command, final Context<T> undoContext) {
        return defaultUndoNestedCommand(command, undoContext);
    }

    /**
     * To rollback changes for contexts with state DONE
     *
     * @param command     nested command to do undo with nested context (could be Override)
     * @param undoContext nested context with DONE state
     * @see AuthorityPersonCommand#undoCommand(Context)
     * @see Context.State#DONE
     * @see Context.State#UNDONE
     * @see Context.State#FAIL
     */
    default <T> Context<T> undoNestedCommand(final AuthorityPersonCommand command, final Context<T> undoContext) {
        return defaultUndoNestedCommand(command, undoContext);
    }

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    Logger getLog();

    private <T> void defaultDoNestedCommand(final SchoolCommand command,
                                            final Context<T> doContext,
                                            final Context.StateChangedListener<T> stateListener) {
        doContext.addStateListener(stateListener);
        final String commandId = command.getId();
        try {
            command.doCommand(doContext);
            getLog().debug("Command:'{}' is done context:{}", commandId, doContext);
        } catch (Exception e) {
            getLog().error("Cannot run do for command:'{}'", commandId, e);
            doContext.failed(e);
        } finally {
            doContext.removeStateListener(stateListener);
        }
    }

    private <T> Context<T> defaultUndoNestedCommand(final SchoolCommand command,
                                                    final Context<T> undoContext) {
        try {
            command.undoCommand(undoContext);
            getLog().debug("Rolled back done command '{}' with context:{}", command.getId(), undoContext);
        } catch (Exception e) {
            getLog().error("Cannot rollback for {}", undoContext, e);
            undoContext.failed(e);
        }
        return undoContext;
    }

    interface Visitable<T> {
        default void doNested(final NestedCommandExecutionVisitor visitor,
                              final SchoolCommand command, final Context<T> doContext,
                              final Context.StateChangedListener<T> stateListener) {
            visitor.doNestedCommand(command, doContext, stateListener);
        }
    }
}
