package oleg.sopilnyak.test.service.command.type;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import oleg.sopilnyak.test.service.command.executable.sys.ParallelMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;

/**
 * Type: Command to execute the couple of school-commands
 *
 * @param <T> the type of command execution (do) result
 * @see RootCommand
 * @see PrepareContextVisitor
 */
public interface CompositeCommand<T> extends RootCommand<T>, PrepareContextVisitor {
    /**
     * To get the collection of nested commands living in the composite
     *
     * @return collection of nested commands
     */
    <N> Collection<NestedCommand<N>> fromNest();

    /**
     * To put the command to the composite's nest
     *
     * @param command the instance to put
     */
    void putToNest(NestedCommand<?> command);

    /**
     * To create command's context of macro command for input
     *
     * @param input root input parameter instance for command's do
     * @return context instance
     * @see CompositeCommand#prepareNestedContexts(CompositeCommand, Input)
     * @see CompositeCommand#lookingForFailed(Deque)
     * @see Input
     * @see Context#failed(Exception)
     * @see RootCommand#createContext(Input)
     * @see RootCommand#createContext()
     */
    @Override
    default Context<T> createContext(final Input<?> input) {
        // preparing contexts for nested commands
        final var contexts = prepareNestedContexts(this, input);
        // looking for failed nested command context
        final var failedContext = lookingForFailed(contexts);
        // according to failed context presents building composite command context
        return failedContext.isEmpty() ?
                RootCommand.super.createContext(Input.of(input, contexts)) :
                RootCommand.super.createContext().failed(failedContext.get().getException());
    }

    // For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(SequentialMacroCommand, Input)
     * @see PrepareContextVisitor#prepareContext(ParallelMacroCommand, Input)
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
    private static Deque<Context<?>> prepareNestedContexts(final CompositeCommand<?> command, final Input<?> input) {
        return command.fromNest().stream()
                .map(nested -> command.prepareNestedContext(nested, input))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    private static Optional<? extends Context<?>> lookingForFailed(final Deque<Context<?>> contexts) {
        return contexts.stream().filter(Objects::nonNull).filter(Context::isFailed).findFirst();
    }

    private Context<?> prepareNestedContext(final NestedCommand<?> command, final Input<?> input) {
        try {
            return command.acceptPreparedContext(this, input);
        } catch (Exception e) {
            getLog().error("Cannot prepare nested command context '{}' for value {}", command, input, e);
            return command.createContextInit().failed(e);
        }
    }
}
