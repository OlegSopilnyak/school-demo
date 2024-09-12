package oleg.sopilnyak.test.service.command.type;

import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.executable.sys.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.executable.sys.ParallelMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import org.springframework.lang.NonNull;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Type: Command to execute the couple of commands
 */
public interface CompositeCommand extends RootCommand, PrepareContextVisitor {
    /**
     * To get the collection of nested commands used it composite
     *
     * @return collection of nested commands
     */
    Collection<NestedCommand> fromNest();

    /**
     * To add the command
     *
     * @param command the instance to add
     */
    void addToNest(NestedCommand command);

    /**
     * To create command's context of macro command for input
     *
     * @param input command context's parameter value
     * @param <T>   type of command result
     * @return context instance
     * @see Context
     * @see Context#getRedoParameter()
     * @see CommandContext
     * @see Context.State#READY
     * @see RootCommand#createContext()
     */
    @Override
    default <T> Context<T> createContext(Object input) {
        final Context<T> macroCommandContext = createContext();
        final Deque<Context<T>> nestedContexts = new LinkedList<>();
        try {
            for (final NestedCommand nestedCommand : fromNest()) {
                final Context<T> nestedContext = nestedCommand.acceptPreparedContext(this, input);
                nestedContexts.add(nestedContext);
            }
            macroCommandContext.setRedoParameter(new MacroCommandParameter<>(input, nestedContexts));
        } catch (Exception e) {
            getLog().error("Cannot create macro command context for {}", input, e);
            macroCommandContext.failed(e);
        }
        return macroCommandContext;
    }

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor visitor of prepared contexts
     * @param input   Macro-Command call's input
     * @param <T>     type of command result
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(SequentialMacroCommand, Object)
     * @see PrepareContextVisitor#prepareContext(ParallelMacroCommand, Object)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Object)
     */
    @Override
    default <T> Context<T> acceptPreparedContext(@NonNull final PrepareContextVisitor visitor, final Object input) {
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
     * @see CompositeCommand#doCommand(Context)
     * @see Context#removeStateListener(Context.StateChangedListener)
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     */
    @Override
    default <T> void doAsNestedCommand(@NonNull final NestedCommandExecutionVisitor visitor,
                                       final Context<T> context, final Context.StateChangedListener<T> stateListener) {
        visitor.doNestedCommand(this, context, stateListener);
    }

    /**
     * To execute command Undo as a nested command
     *
     * @param visitor visitor to do nested command execution
     * @param context context for nested command execution
     * @param <T>     type of command execution result
     * @see NestedCommandExecutionVisitor#undoNestedCommand(RootCommand, Context)
     * @see CompositeCommand#undoCommand(Context)
     */
    @Override
    default <T> Context<T> undoAsNestedCommand(@NonNull final NestedCommandExecutionVisitor visitor,
                                               final Context<T> context) {
        return visitor.undoNestedCommand(this, context);
    }
}
