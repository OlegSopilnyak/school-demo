package oleg.sopilnyak.test.service.command.type.base;

import static java.util.Objects.isNull;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.sys.ParallelMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.io.Input;
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
     * To get action-executor for nested commands processing
     *
     * @return instance of the executor
     */
    default ActionExecutor getActionExecutor() {
        throw new UnsupportedOperationException("ActionExecutor instance should be reachable");
    }

    /**
     * To get the collection of commands live in the composite's nest
     *
     * @return collection of nested commands
     */
    Collection<NestedCommand<?>> fromNest();

    /**
     * To put the command to the composite's nest
     *
     * @param command the instance to put
     * @return true if added
     * @param <N> the result type of the nested command
     */
    <N> boolean putToNest(NestedCommand<N> command);

    /**
     * To build command's context of composite command for the input
     *
     * @param input root input parameter instance for command execution
     * @return built context instance
     * @see Input
     * @see Context#isFailed()
     * @see RootCommand#createContext(Input)
     * @see RootCommand#createContext()
     * @see NestedCommand#createFailedContext(Exception)
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
                .map(failedContext -> RootCommand.super.createFailedContext(failedContext.getException()))
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
    @SuppressWarnings("unchecked")
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

    /**
     * To execute nested do command with the nested context
     *
     * @param <N>     the type of nested command execution result
     * @param context the context used for use with the nested command
     * @return context after nested command do
     * @see CompositeCommand#executeDoNested(Context, Context.StateChangedListener)
     */
    default <N> Context<N> executeDoNested(final Context<N> context) {
        return executeDoNested(context, null);
    }

    /**
     * To execute do of nested command with the nested context and context-state-change listener
     *
     * @param <N>      the type of nested command execution result
     * @param context  the context used for use with the nested command
     * @param listener which will be notified by new states after do execution
     * @return context after nested command do
     */
    default <N> Context<N> executeDoNested(final Context<N> context, final Context.StateChangedListener listener) {
        if (isNull(listener)) {
            // execute nested context using action executor
            return getActionExecutor().commitAction(ActionContext.current(), context);
        }
        // store states before do execution
        final Deque<Context.State> statesBefore = context.getHistory().states();
        //
        // execute nested context using action executor
        Context<N> result;
        try {
            result = getActionExecutor().commitAction(ActionContext.current(), context);
        } catch (Exception e) {
            result = context.failed(e);
            getLog().error("Cannot commit nested context using action executor...", e);
        }
        //
        // notifying context state-change-listener by new states after do execution
        final Deque<Context.State> statesAfter = new LinkedList<>(context.getHistory().states());
        if (statesAfter.removeAll(statesBefore) && !statesAfter.isEmpty()) {
            // notifying passed state-change listener
            notifyStateChangeListener(listener, result, statesAfter, statesBefore.getLast());
        }
        return result;
    }

    private static <N> void notifyStateChangeListener(final Context.StateChangedListener listener,
                                                      final Context<N> result,
                                                      final Deque<Context.State> statesAfter,
                                                      final Context.State lastBeforeState) {
        // prepare previous state
        final AtomicReference<Context.State> previous = new AtomicReference<>(lastBeforeState);
        // iterating after execution context states
        statesAfter.forEach(current -> {
            // apply state changes to passed state-change-listener instance
            listener.stateChanged(result, previous.get(), current);
            // set up previous state to current one
            previous.getAndSet(current);
        });
    }

    /**
     * To run do execution for each macro-command's nested contexts in READY state
     *
     * @param contexts nested contexts to execute
     * @param listener listener of nested context-state-change
     * @return nested contexts after execution
     * @see Context.State#READY
     * @see Deque
     * @see Context.StateChangedListener
     * @see CompositeCommand#executeDoNested(Context, Context.StateChangedListener)
     */
    Deque<Context<?>> executeNested(Deque<Context<?>> contexts, Context.StateChangedListener listener);

    /**
     * To execute undo of nested command with the nested context
     *
     * @param context the context used for use with the nested command
     * @return context after nested command undo
     */
    default Context<Void> executeUndoNested(final Context<Void> context) {
        try{
            // execute rollback for nested context using action executor
            return getActionExecutor().rollbackAction(ActionContext.current(), context);
        } catch (Exception e) {
            getLog().error("Cannot rollback nested context using action executor...", e);
            return context.failed(e);
        }
    }

    /**
     * To run undo execution for each macro-command's nested contexts in DONE state
     *
     * @param contexts nested contexts to execute
     * @return nested contexts after execution
     * @see Context.State#DONE
     * @see Deque
     * @see CompositeCommand#executeUndoNested(Context)
     */
    Deque<Context<?>> rollbackNested(Deque<Context<?>> contexts);
}
