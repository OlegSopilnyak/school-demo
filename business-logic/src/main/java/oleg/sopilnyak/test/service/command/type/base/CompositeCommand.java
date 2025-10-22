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
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;

/**
 * Type: Command to execute the couple of nested school-commands
 *
 * @param <T> the type of command execution result
 * @see RootCommand
 * @see PrepareNestedContextVisitor
 */
public interface CompositeCommand<T> extends RootCommand<T>, PrepareNestedContextVisitor {
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
     * @see PrepareNestedContextVisitor#prepareContext(SequentialMacroCommand, Input)
     * @see PrepareNestedContextVisitor#prepareContext(ParallelMacroCommand, Input)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareNestedContextVisitor visitor, final Input<?> mainInputParameter) {
        return visitor.prepareContext(this, mainInputParameter);
    }

    /**
     * To run DO execution for each macro-command's nested command-contexts
     *
     * @param contexts nested contexts to execute
     * @param listener listener of nested context-state-change
     * @return nested contexts after DO execution
     * @see Deque
     * @see Context.StateChangedListener
     * @see CompositeCommand#executeDoNested(Context, Context.StateChangedListener)
     */
    default Deque<Context<?>> executeNested(Deque<Context<?>> contexts, Context.StateChangedListener listener) {
        return contexts.stream()
                // execute do for nested command's context
                .map(context -> executeDoNested(context, listener))
                // prepare result as Deque
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * To execute DO of nested command with the nested context and context-state-change listener through action-executor
     *
     * @param <N>      the type of nested command execution result
     * @param context  the context used for use with the nested command
     * @param listener which will be notified by new states after do execution
     * @return context after nested command do
     * @see CompositeCommand#executeNested(Deque, Context.StateChangedListener)
     * @see ActionExecutor#commitAction(ActionContext, Context)
     * @see Context
     * @see Context#getHistory()
     * @see Context.State
     * @see Context.LifeCycleHistory#states()
     * @see Context.StateChangedListener
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
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
        // notifying context the state-change-listener by new states after DO execution
        final Deque<Context.State> statesAfter = new LinkedList<>(context.getHistory().states());
        if (statesAfter.removeAll(statesBefore) && !statesAfter.isEmpty()) {
            //
            // prepare previous state and context after DO execution
            final AtomicReference<Context.State> previous = new AtomicReference<>(statesBefore.getLast());
            final Context<N> contextAfterCommandDo = result;
            //
            // notifying by new states state-change listener instances
            // iterating after DO execution context states
            statesAfter.forEach(current -> {
                // apply new state changes to the state-change-listener instance
                listener.stateChanged(contextAfterCommandDo, previous.get(), current);
                // set up previous state to current one
                previous.getAndSet(current);
            });
        }
        return result;
    }

    /**
     * To execute DO of nested command with the nested context only
     *
     * @param <N>     the type of nested command execution result
     * @param context the context used for use with the nested command
     * @return context after nested command do
     * @see CompositeCommand#executeDoNested(Context, Context.StateChangedListener)
     * @see Context.StateChangedListener
     */
    default <N> Context<N> executeDoNested(final Context<N> context) {
        return executeDoNested(context, null);
    }

    /**
     * To run UNDO execution for each macro-command's nested contexts in DONE state
     *
     * @param contexts nested contexts to execute
     * @return nested contexts after execution
     * @see Context#isDone()
     * @see Deque
     * @see CompositeCommand#executeUndoNested(Context)
     */
    default Deque<Context<?>> rollbackNested(Deque<Context<?>> contexts) {
        return contexts.stream()
                // rollback only for contexts in DONE state
                .filter(Context::isDone)
                // execute rollback for the nested command's context
                .map(this::executeUndoNested)
                // prepare result as Deque
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * To execute UNDO of nested command with the nested context
     *
     * @param context the context used for use with the nested command
     * @return context after nested command undo
     * @see CompositeCommand#rollbackNested(Deque)
     */
    default Context<?> executeUndoNested(final Context<?> context) {
        try{
            // execute rollback for nested context using action executor
            return getActionExecutor().rollbackAction(ActionContext.current(), context);
        } catch (Exception e) {
            getLog().error("Cannot rollback nested context using action executor...", e);
            return context.failed(e);
        }
    }
}
