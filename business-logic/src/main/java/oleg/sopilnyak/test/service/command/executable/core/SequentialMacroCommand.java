package oleg.sopilnyak.test.service.command.executable.core;

import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.type.core.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.exception.CannotTransferCommandResultException;

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
    protected SequentialMacroCommand(CommandActionExecutor actionExecutor) {
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
        // the holder of just now executed nested command-context with execution result
        final AtomicReference<Context<?>> executedCommandContextHolder = new AtomicReference<>(null);
        // sequential walking through nested command-contexts collection
        return contexts.stream().map(context -> isPreviousFailed.get()
                // previous nested command's context.state has value FAIL? Cancel it
                ? cancelSequentialNestedCommandContext(context, listener)
                // try to execute nested command
                : doSequentialNestedCommand(context, executedCommandContextHolder, listener, isPreviousFailed)
        ).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * To mark canceled nested command execution
     *
     * @param toCancel the nested command-context to cancel
     * @param listener the listener of context-state-changes
     * @return canceled command-context
     * @see Context.State#CANCEL
     */
    protected Context<?> cancelSequentialNestedCommandContext(
            final Context<?> toCancel, final Context.StateChangedListener listener
    ) {
        // getting last state from the context history and use it for the listener's notification
        final Context.State lastState = toCancel.getHistory().states().getLast();
        getLog().debug("Cancel nested command execution from state {}", lastState);
        // update context-state-changes listener
        listener.stateChanged(toCancel, lastState, Context.State.CANCEL);
        // update context state to CANCEL
        toCancel.setState(Context.State.CANCEL);
        return toCancel;
    }

    /**
     * To execute nested command using command-context
     *
     * @param toExecute         the next nested command-context to execute
     * @param executedHolder    holder of previously executed nested command-context
     * @param listener          the listener of context-state-changes
     * @param isExecutionFailed holder of previous nested command execution failed flag
     * @param <N>               type of to-execute nested command result
     * @return command-context value after to-execute nested command execution
     * @see Context#isDone()
     * @see Context#getResult()
     * @see java.util.Optional#orElse(Object)
     */
    protected <N> Context<N> doSequentialNestedCommand(
            final Context<N> toExecute, final AtomicReference<Context<?>> executedHolder,
            final Context.StateChangedListener listener, final AtomicBoolean isExecutionFailed
    ) {
        final Context<?> executed = executedHolder.get();
        if (Objects.nonNull(executed)) {
            getLog().debug("Transferring the result to the current nested command context of '{}'", toExecute.getCommand().getId());
            // transferring result-data from executed command-context-result to the current command-context
            transferResult(executed.getCommand(), executed.getResult().orElse(null), toExecute);
        }

        // executing nested command using current command-context
        getLog().debug("Executing nested command for Command-Context:{}", toExecute);
        final Context<N> context = executeDoNested(toExecute, listener);

        // checking nested command execution result's state
        if (context.isDone()) {
            //
            // current nested command execution is successful
            // getting command execution result to log
            getLog().debug("Command with id:'{}' is executed well, with result {}",
                    context.getCommand().getId(),
                    context.getResult().orElse(null)
            );
            // store well executed command-context to the holder
            executedHolder.getAndSet(context);
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
     * To transfer result of executed command to the next nested command in sequence
     *
     * @param executedCommand the command-owner of transferred result
     * @param toTransfer      the result of executed nested command
     * @param toExecute       the next nested command-context to execute in the sequence
     */
    public void transferResult(
            final RootCommand<?> executedCommand, final Object toTransfer, final Context<?> toExecute
    ) {
        final String commandId = toExecute.getCommand().getId();
        getLog().error("Please implement transferResult(command,result,context) method for command with ID:'{}'", commandId);
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
        final AtomicBoolean isFailedHolder = new AtomicBoolean(false);
        // rollback nested commands changes in the reverse order
        final Deque<Context<?>> rolledBack = List.copyOf(contexts).reversed().stream()
                .map(context -> rollbackNestedCommand(context, isFailedHolder))
                .collect(Collectors.toCollection(LinkedList::new));
        // reverse the order of rolled back nested command contexts back to the original order
        return rolledBack.reversed();
    }

    /**
     * To rollback nested command execution
     *
     * @param current                        command-context after nested command execution
     * @param isPreviousRollbackFailedHolder the holder of failed nested command's rollback flag
     * @return nested command-context after nested command's rollback
     */
    protected Context<?> rollbackNestedCommand(
            final Context<?> current, final AtomicBoolean isPreviousRollbackFailedHolder
    ) {
        if (isPreviousRollbackFailedHolder.get()) {
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
            isPreviousRollbackFailedHolder.compareAndSet(false, true);
        } else {
            // nested command rollback is successful
            getLog().debug("Rollback of the nested command '{}' is completed successfully", commandId);
        }
        // returning nested command's rollback context
        return context;
    }
}
