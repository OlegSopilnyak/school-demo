package oleg.sopilnyak.test.service.command.type;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
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
 * Type: Command to execute the couple of nested school-commands
 *
 * @param <T> the type of command execution result
 * @see RootCommand
 * @see PrepareContextVisitor
 */
public interface CompositeCommand<T> extends RootCommand<T>, PrepareContextVisitor {
    /**
     * To get the collection of commands live in the composite's nest
     *
     * @return collection of nested commands
     */
    <N> Collection<NestedCommand<N>> fromNest();

    /**
     * To put the command to the composite's nest
     *
     * @param command the instance to put
     * @return true if added
     */
    boolean putToNest(NestedCommand<?> command);

    /**
     * To build command's context of composite command for input
     *
     * @param input root input parameter instance for command execution
     * @return built context instance
     * @see Input
     * @see Context#failed(Exception)
     * @see RootCommand#createContext(Input)
     * @see RootCommand#createContext()
     */
    @Override
    default Context<T> createContext(final Input<?> input) {
        // preparing contexts for the nested commands of the composite command
        final Deque<Context<?>> contexts = fromNest().stream()
                .map(nestedCommand -> {
                    try {
                        return nestedCommand.acceptPreparedContext(this, input);
                    } catch (Exception e) {
                        getLog().error("Cannot prepare nested command context '{}' for value {}", nestedCommand, input, e);
                        return nestedCommand.createFailedContext(e);
                    }
                }).collect(Collectors.toCollection(LinkedList::new));
        // looking for failed nested command context after nested command context preparing
        return contexts.stream().filter(Objects::nonNull).filter(Context::isFailed).findFirst()
                // failed nested command context found, creating failed main command-context with exception
                .map(fail -> RootCommand.super.createFailedContext(fail.getException()))
                // nested contexts preparation was fine, creating ready to use main command-context
                .orElseGet(() -> RootCommand.super.createContext(Input.of(input, contexts)));
    }

    // -----  For commands playing Nested Command Role -----

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor            visitor to make prepared contexts for nested command
     * @param mainInputParameter composite command call's input
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(SequentialMacroCommand, Input)
     * @see PrepareContextVisitor#prepareContext(ParallelMacroCommand, Input)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareContextVisitor visitor, final Input<?> mainInputParameter) {
        return visitor.prepareContext(this, mainInputParameter);
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
}
