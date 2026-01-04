package oleg.sopilnyak.test.service.command.type.core.nested.legacy;

import oleg.sopilnyak.test.service.command.type.core.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.organization.FacultyCommand;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;

import org.slf4j.Logger;

/**
 * Visitor: Executing nested command actions.
 * Any method can be overridden in the class-child
 */
@Deprecated
public interface NestedCommandExecutionVisitor {
    /**
     * To execute nested command (RootCommand)
     *
     * @param command       nested command to be executed
     * @param doContext     context for execution
     * @param stateListener the lister of command-context-state changing
     * @param <N>           type of nested command execution result
     * @see RootCommand
     * @see NestedCommandExecutionVisitor#defaultDoNestedCommand(RootCommand, Context, Context.StateChangedListener)
     */
    default <N> void doNestedCommand(final RootCommand<N> command,
                                     final Context<N> doContext, final Context.StateChangedListener stateListener) {
        defaultDoNestedCommand(command, doContext, stateListener);
    }

    /**
     * To execute nested command (StudentCommand)
     *
     * @param command       nested command to be executed
     * @param doContext     context for execution
     * @param stateListener the lister of command-context-state changing
     * @param <N>           type of nested command execution result
     * @see StudentCommand
     * @see NestedCommandExecutionVisitor#defaultDoNestedCommand(RootCommand, Context, Context.StateChangedListener)
     */
    default <N> void doNestedCommand(final StudentCommand<N> command,
                                     final Context<N> doContext, final Context.StateChangedListener stateListener) {
        defaultDoNestedCommand(command, doContext, stateListener);
    }

    /**
     * To execute nested command (CourseCommand)
     *
     * @param command       nested command to be executed
     * @param doContext     context for execution
     * @param stateListener the lister of command-context-state changing
     * @param <N>           type of nested command execution result
     * @see CourseCommand
     * @see NestedCommandExecutionVisitor#defaultDoNestedCommand(RootCommand, Context, Context.StateChangedListener)
     */
    default <N> void doNestedCommand(final CourseCommand<N> command,
                                     final Context<N> doContext, final Context.StateChangedListener stateListener) {
        defaultDoNestedCommand(command, doContext, stateListener);
    }

    /**
     * To execute nested command (CompositeCommand)
     *
     * @param command       nested command to be executed
     * @param doContext     context for execution
     * @param stateListener the lister of command-context-state changing
     * @param <N>           type of nested command execution result
     * @see CompositeCommand
     * @see NestedCommandExecutionVisitor#defaultDoNestedCommand(RootCommand, Context, Context.StateChangedListener)
     */
    default <N> void doNestedCommand(final CompositeCommand<N> command,
                                     final Context<N> doContext,  final Context.StateChangedListener stateListener) {
        defaultDoNestedCommand(command, doContext, stateListener);
    }

    /**
     * To execute nested command (StudentProfileCommand)
     *
     * @param command       nested command to be executed
     * @param doContext     context for execution
     * @param stateListener the lister of command-context-state changing
     * @param <N>           type of nested command execution result
     * @see StudentProfileCommand
     * @see NestedCommandExecutionVisitor#defaultDoNestedCommand(RootCommand, Context, Context.StateChangedListener)
     */
    default <N> void doNestedCommand(final StudentProfileCommand<N> command,
                                     final Context<N> doContext, final Context.StateChangedListener stateListener) {
        defaultDoNestedCommand(command, doContext, stateListener);
    }

    /**
     * To execute nested command (PrincipalProfileCommand)
     *
     * @param command       nested command to be executed
     * @param doContext     context for execution
     * @param stateListener the lister of command-context-state changing
     * @param <N>           type of nested command execution result
     * @see PrincipalProfileCommand
     * @see NestedCommandExecutionVisitor#defaultDoNestedCommand(RootCommand, Context, Context.StateChangedListener)
     */
    default <N> void doNestedCommand(final PrincipalProfileCommand<N> command,
                                     final Context<N> doContext, final Context.StateChangedListener stateListener) {
        defaultDoNestedCommand(command, doContext, stateListener);
    }

    /**
     * To execute nested command (AuthorityPersonCommand)
     *
     * @param command       nested command to be executed
     * @param doContext     context for execution
     * @param stateListener the lister of command-context-state changing
     * @param <N>           type of nested command execution result
     * @see AuthorityPersonCommand
     * @see NestedCommandExecutionVisitor#defaultDoNestedCommand(RootCommand, Context, Context.StateChangedListener)
     */
    default <N> void doNestedCommand(final AuthorityPersonCommand<N> command,
                                     final Context<N> doContext, final Context.StateChangedListener stateListener) {
        defaultDoNestedCommand(command, doContext, stateListener);
    }

    /**
     * To execute nested command (FacultyCommand)
     *
     * @param command       nested command to be executed
     * @param doContext     context for execution
     * @param stateListener the lister of command-context-state changing
     * @param <N>           type of nested command execution result
     * @see FacultyCommand
     * @see NestedCommandExecutionVisitor#defaultDoNestedCommand(RootCommand, Context, Context.StateChangedListener)
     */
    default <N> void doNestedCommand(final FacultyCommand<N> command,
                                     final Context<N> doContext, final Context.StateChangedListener stateListener) {
        defaultDoNestedCommand(command, doContext, stateListener);
    }

    /**
     * To execute nested command (StudentsGroupCommand)
     *
     * @param command       nested command to be executed
     * @param doContext     context for execution
     * @param stateListener the lister of command-context-state changing
     * @param <N>           type of nested command execution result
     * @see StudentsGroupCommand
     * @see NestedCommandExecutionVisitor#defaultDoNestedCommand(RootCommand, Context, Context.StateChangedListener)
     */
    default <N> void doNestedCommand(final StudentsGroupCommand<N> command,
                                     final Context<N> doContext, final Context.StateChangedListener stateListener) {
        defaultDoNestedCommand(command, doContext, stateListener);
    }

    /**
     * To rollback changes for context with state DONE command (RootCommand)
     * <BR/> the type of command result doesn't matter
     *
     * @param command     nested command to do undo with nested context (could be Overridden)
     * @param undoContext nested command context with DONE state
     * @return nested command context with undo results
     * @see RootCommand
     * @see NestedCommandExecutionVisitor#defaultUndoNestedCommand(RootCommand, Context)
     */
    default Context<?> undoNestedCommand(final RootCommand<?> command, final Context<?> undoContext) {
        return defaultUndoNestedCommand(command, undoContext);
    }

    /**
     * To rollback changes for nested context with state DONE command (StudentCommand)
     * <BR/> the type of command result doesn't matter
     *
     * @param command     nested command to do undo with nested context (could be Overridden)
     * @param undoContext nested command context with DONE state
     * @return nested command context with undo results
     * @see StudentCommand
     * @see NestedCommandExecutionVisitor#defaultUndoNestedCommand(RootCommand, Context)
     */
    default Context<?> undoNestedCommand(final StudentCommand<?> command, final Context<?> undoContext) {
        return defaultUndoNestedCommand(command, undoContext);
    }

    /**
     * To rollback changes for nested context with state DONE command (CourseCommand)
     * <BR/> the type of command result doesn't matter
     *
     * @param command     nested command to do undo with nested context (could be Overridden)
     * @param undoContext nested command context with DONE state
     * @return nested command context with undo results
     * @see CourseCommand
     * @see NestedCommandExecutionVisitor#defaultUndoNestedCommand(RootCommand, Context)
     */
    default Context<?> undoNestedCommand(final CourseCommand<?> command, final Context<?> undoContext) {
        return defaultUndoNestedCommand(command, undoContext);
    }

    /**
     * To rollback changes for nested context with state DONE command (CompositeCommand)
     * <BR/> the type of command result doesn't matter
     *
     * @param command     nested command to do undo with nested context (could be Overridden)
     * @param undoContext nested command context with DONE state
     * @return nested command context with undo results
     * @see CompositeCommand
     * @see NestedCommandExecutionVisitor#defaultUndoNestedCommand(RootCommand, Context)
     */
    default Context<?> undoNestedCommand(final CompositeCommand<?> command, final Context<?> undoContext) {
        return defaultUndoNestedCommand(command, undoContext);
    }

    /**
     * To rollback changes for nested context with state DONE command (StudentProfileCommand)
     * <BR/> the type of command result doesn't matter
     *
     * @param command     nested command to do undo with nested context (could be Overridden)
     * @param undoContext nested command context with DONE state
     * @return nested command context with undo results
     * @see StudentProfileCommand
     * @see NestedCommandExecutionVisitor#defaultUndoNestedCommand(RootCommand, Context)
     */
    default Context<?> undoNestedCommand(final StudentProfileCommand<?> command, final Context<?> undoContext) {
        return defaultUndoNestedCommand(command, undoContext);
    }

    /**
     * To rollback changes for nested context with state DONE command (PrincipalProfileCommand)
     * <BR/> the type of command result doesn't matter
     *
     * @param command     nested command to do undo with nested context (could be Overridden)
     * @param undoContext nested command context with DONE state
     * @return nested command context with undo results
     * @see PrincipalProfileCommand
     * @see NestedCommandExecutionVisitor#defaultUndoNestedCommand(RootCommand, Context)
     */
    default Context<?> undoNestedCommand(final PrincipalProfileCommand<?> command, final Context<?> undoContext) {
        return defaultUndoNestedCommand(command, undoContext);
    }

    /**
     * To rollback changes for nested context with state DONE command (AuthorityPersonCommand)
     * <BR/> the type of command result doesn't matter
     *
     * @param command     nested command to do undo with nested context (could be Overridden)
     * @param undoContext nested command context with DONE state
     * @return nested command context with undo results
     * @see AuthorityPersonCommand
     * @see NestedCommandExecutionVisitor#defaultUndoNestedCommand(RootCommand, Context)
     */
    default Context<?> undoNestedCommand(final AuthorityPersonCommand<?> command, final Context<?> undoContext) {
        return defaultUndoNestedCommand(command, undoContext);
    }

    /**
     * To rollback changes for nested context with state DONE command (FacultyCommand)
     * <BR/> the type of command result doesn't matter
     *
     * @param command     nested command to do undo with nested context (could be Overridden)
     * @param undoContext nested command context with DONE state
     * @return nested command context with undo results
     * @see FacultyCommand
     * @see NestedCommandExecutionVisitor#defaultUndoNestedCommand(RootCommand, Context)
     */
    default Context<?> undoNestedCommand(final FacultyCommand<?> command, final Context<?> undoContext) {
        return defaultUndoNestedCommand(command, undoContext);
    }

    /**
     * To rollback changes for nested context with state DONE command (StudentsGroupCommand)
     * <BR/> the type of command result doesn't matter
     *
     * @param command     nested command to do undo with nested context (could be Overridden)
     * @param undoContext nested command context with DONE state
     * @return nested command context with undo results
     * @see StudentsGroupCommand
     * @see NestedCommandExecutionVisitor#defaultUndoNestedCommand(RootCommand, Context)
     */
    default Context<?> undoNestedCommand(final StudentsGroupCommand<?> command, final Context<?> undoContext) {
        return defaultUndoNestedCommand(command, undoContext);
    }

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    Logger getLog();

    // private methods

    /**
     * Default behavior of do-nested-command activity
     *
     * @param command       command to execute
     * @param doContext     execution context
     * @param stateListener listener of context-state changing
     * @param <N>           type of command execution result
     * @see RootCommand#doCommand(Context)
     * @see Context#addStateListener(Context.StateChangedListener)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     * @see Context.State#READY
     * @see Context.State#DONE
     * @see Context.State#FAIL
     * @see Context#failed(Exception)
     */
    private <N> void defaultDoNestedCommand(final RootCommand<N> command,
                                            final Context<N> doContext,
                                            final Context.StateChangedListener stateListener) {
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

    /**
     * Default behavior of undo-nested-command activity
     *
     * @param command     nested command to do undo with nested context
     * @param undoContext nested command context with DONE state
     * @return nested command context with undo results
     * @see RootCommand#undoCommand(Context)
     * @see Context.State#DONE
     * @see Context.State#UNDONE
     * @see Context.State#FAIL
     * @see Context#failed(Exception)
     */
    private Context<?> defaultUndoNestedCommand(RootCommand<?> command, Context<?> undoContext) {
        try {
            command.undoCommand(undoContext);
            getLog().debug("Rolled back done command '{}' with context:{}", command.getId(), undoContext);
        } catch (Exception e) {
            getLog().error("Cannot rollback command '{}' for {}", command.getId(), undoContext, e);
            undoContext.failed(e);
        }
        return undoContext;
    }

}
