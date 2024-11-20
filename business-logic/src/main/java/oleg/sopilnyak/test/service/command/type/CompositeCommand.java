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

import java.util.*;
import java.util.stream.Collectors;

/**
 * Type: Command to execute the couple of school-commands
 *
 * @param <T> the type of command execution (do) result
 * @see RootCommand
 * @see PrepareContextVisitor
 */
public interface CompositeCommand<T> extends RootCommand<T>, PrepareContextVisitor {
    /**
     * To get the collection of nested commands used it composite commands
     *
     * @return collection of nested commands
     */
    <N> Collection<NestedCommand<N>> fromNest();

    /**
     * To add the command
     *
     * @param command the instance to add
     */
    void addToNest(NestedCommand<?> command);

    /**
     * To create command's context of macro command for input
     *
     * @param inputParameter command context's parameter value
     *              //     * @param <T>   type of command result
     * @return context instance
     * @see Context
     * @see Context#getRedoParameter()
     * @see CommandContext
     * @see Context.State#READY
     * @see RootCommand#createContext()
     */
    @Override
    default Context<T> createContext(Object inputParameter) {
        final Deque<Context<?>> nested = fromNest().stream()
                .map(nestedCommand -> prepareNestedContext(nestedCommand, inputParameter))
                .collect(Collectors.toCollection(LinkedList::new));
        final Optional<? extends Context<?>> failed =
                nested.stream().filter(Objects::nonNull).filter(Context::isFailed).findFirst();
        final Context<T> macroCommandContext = createContext();
        if (failed.isPresent()) {
            macroCommandContext.failed(failed.get().getException());
        } else {
            final var redoParameter = new MacroCommandParameter(inputParameter, nested);
            macroCommandContext.setRedoParameter(redoParameter);
        }
        return macroCommandContext;
    }

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor        visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input
     *                       //     * @param <T>     type of command result
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(SequentialMacroCommand, Object)
     * @see PrepareContextVisitor#prepareContext(ParallelMacroCommand, Object)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Object)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareContextVisitor visitor, final Object macroInputParameter) {
        return visitor.prepareContext(this, macroInputParameter);
    }

    /**
     * To execute command Do as a nested command
     *
     * @param visitor       visitor to do nested command execution
     * @param context       context for nested command execution
     * @param stateListener listener of context-state-change
     * @see NestedCommandExecutionVisitor#doNestedCommand(CompositeCommand, Context, Context.StateChangedListener)
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
     * @see CompositeCommand#undoCommand(Context)
     */
    @Override
    default Context<?> undoAsNestedCommand(final NestedCommandExecutionVisitor visitor, final Context<?> context) {
        return visitor.undoNestedCommand(this, context);
    }

    // private methods
    private Context<?> prepareNestedContext(NestedCommand<?> command, Object mainInputParameter) {
        try {
            return command.acceptPreparedContext(this, mainInputParameter);
        } catch (Exception e) {
            getLog().error("Cannot prepare nested command context '{}' for value {}", command, mainInputParameter, e);
            return command.createContextInit().failed(e);
        }
    }
}
