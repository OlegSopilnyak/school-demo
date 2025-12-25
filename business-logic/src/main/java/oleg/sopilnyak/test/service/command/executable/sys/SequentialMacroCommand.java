package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.type.base.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.exception.CannotTransferCommandResultException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.springframework.util.ObjectUtils;

/**
 * MacroCommand: The command type with nested commands inside which are executing in sequential way.
 *
 * @param <T> the type of command execution result
 * @see CompositeCommand
 * @see MacroCommand
 */
public abstract class SequentialMacroCommand<T> extends MacroCommand<T> {
    protected SequentialMacroCommand(ActionExecutor actionExecutor) {
        super(actionExecutor);
    }

    /**
     * To run macro-command's nested command-contexts<BR/>
     * Executing sequence of nested command-contexts
     *
     * @param contexts nested command contexts deque to do
     * @param listener listener of the nested command context state-change
     * @return nested command-contexts deque after execution of all nested commands
     * @see SequentialMacroCommand#cancelSequentialNestedCommandContext(Context, Context.StateChangedListener)
     * @see SequentialMacroCommand#doSequentialNestedCommand(Context, AtomicReference, Context.StateChangedListener, AtomicBoolean)
     * @see AtomicReference
     * @see AtomicBoolean
     */
    @Override
    public Deque<Context<?>> executeNested(final Deque<Context<?>> contexts, final Context.StateChangedListener listener) {
        if (ObjectUtils.isEmpty(contexts)) {
            getLog().warn("Nothing to do");
            return contexts;
        }
        // holder of flag if something went wrong during execution
        final AtomicBoolean isPreviousFailed = new AtomicBoolean(false);
        // holder of last executed nested command result value
        final AtomicReference<Object> previousResultHolder = new AtomicReference<>(null);
        // sequential walking through command-contexts collection
        return contexts.stream().map(context -> isPreviousFailed.get()
                // previous nested command's context.state has value FAIL? Cancel it
                ? cancelSequentialNestedCommandContext(context, listener)
                // try to execute nested command
                : doSequentialNestedCommand(context, previousResultHolder, listener, isPreviousFailed)
        ).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * To mark canceled nested command execution
     *
     * @param context the command-context to cancel
     * @param listener the listener of context-state-changes
     * @return canceled command-context
     * @see Context.State#CANCEL
     */
    protected Context<?> cancelSequentialNestedCommandContext(final Context<?> context, final Context.StateChangedListener listener) {
        // getting last state from the context history and use it for the listener's notification
        final Context.State lastState = context.getHistory().states().getLast();
        getLog().debug("Cancel nested command execution from state {}", lastState);
        // update context-state-changes listener
        listener.stateChanged(context, lastState, Context.State.CANCEL);
        // update context state to CANCEL
        context.setState(Context.State.CANCEL);
        return context;
    }

    /**
     * To execute nested command using command-context
     *
     * @param current the command-context of nested command to execute
     * @param executionResultHolder holder of previous execution result
     * @param listener the listener of context-state-changes
     * @param isExecutionFailed holder of previous nested command execution success flag
     * @param <N> type of nested command execution result
     * @return command-context value after nested command execution
     * @see Context#isDone()
     * @see Context#getResult()
     * @see java.util.Optional#orElse(Object)
     */
    protected <N> Context<N> doSequentialNestedCommand(
            final Context<N> current, final AtomicReference<Object> executionResultHolder,
            final Context.StateChangedListener listener, final AtomicBoolean isExecutionFailed
    ) {
        final Object previousExecutionResult = executionResultHolder.get();
        if (Objects.nonNull(previousExecutionResult)) {
            getLog().debug("Transferring the result to the current nested command context of '{}'", current.getCommand().getId());
            // transferring result-data from previous execution to the current command-context
            transferResultForward(previousExecutionResult, current);
        }

        // executing nested command using current command-context
        getLog().debug("Executing nested command for Command-Context:{}", current);
        final Context<N> context = executeDoNested(current, listener);

        // checking nested command execution result's state
        if (context.isDone()) {
            //
            // current nested command execution is successful
            // getting command execution result to store in the result holder
            final Object executionResult = context.getResult().orElse(null);
            getLog().debug("Previous Command is executed well, with result {}", executionResult);
            executionResultHolder.getAndSet(executionResult);
        } else {
            //
            // current nested command execution is failed
            getLog().warn("=== Something went wrong. Command-Context-State:{}" +
                            " Canceling the rest of nested commands in the sequence.",
                    context.getState());
            // set failed flag to true
            isExecutionFailed.compareAndSet(false, true);
        }
        // returning nested command's execution context
        return context;
    }

    /**
     * To transfer result of the previous command execution to the next command-context
     *
     * @param result  result of previous command execution value
     * @param context the command-context of the next command in sequence
     */
    public void transferResultForward(Object result, Context<?> context) {
        final String commandId = context.getCommand().getId();
        getLog().error("Please implement transferResultForward(result,context) method for command with ID:'{}'", commandId);
        throw new CannotTransferCommandResultException(commandId);
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
    @Override
    public Deque<Context<?>> rollbackNested(final Deque<Context<?>> contexts) {
        final List<Context<?>> reverted = new ArrayList<>(contexts);
        final AtomicBoolean isNestedRollbackFailed = new AtomicBoolean(false);
        // reverse the order of undo nested command contexts
        Collections.reverse(reverted);
        // rollback nested commands changes in the reverse order
        final LinkedList<Context<?>> rolledBack = reverted.stream()
                .map(context -> rollbackNestedCommand(context, isNestedRollbackFailed))
                .collect(Collectors.toCollection(LinkedList::new));
        // reverse the order of rolled back nested command contexts back to the original order
        Collections.reverse(rolledBack);
        return rolledBack;
    }

    /**
     * To rollback nested command execution
     *
     * @param current command-context after nested command execution
     * @param rollbackFailedHolder the holder of failed rollback flag
     * @return command-context after nested command rollback
     */
    protected Context<?> rollbackNestedCommand(final Context<?> current, final AtomicBoolean rollbackFailedHolder) {
        if (rollbackFailedHolder.get()) {
            getLog().debug("Skipping current nested rollback because previous one was failed");
            return current;
        }

        getLog().debug("Rolling back nested command execution for '{}'", current.getCommand().getId());
        // rollback nested command
        final Context<?> context = executeUndoNested(current);

        // check rollback result
        final String commandId = context.getCommand().getId();
        if (context.isFailed()) {
            // nested command rollback is failed
            getLog().warn("=== Rollback of the nested command '{}' is failed", commandId, context.getException());
            rollbackFailedHolder.compareAndSet(false, true);
        } else {
            // nested command rollback is successful
            getLog().debug("Rollback of the nested command '{}' is completed successfully", commandId);
        }
        // returning nested command's rollback context
        return context;
    }
}
