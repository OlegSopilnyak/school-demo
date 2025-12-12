package oleg.sopilnyak.test.service.command.executable.sys;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.type.base.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.CommandInSequence;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.TransferTransitionalResultVisitor;
import oleg.sopilnyak.test.service.exception.CannotTransferCommandResultException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.util.ObjectUtils;

/**
 * MacroCommand: The command type with nested commands inside which are executing in sequential way.
 *
 * @param <T> the type of command execution (do) result
 * @see CompositeCommand
 * @see MacroCommand
 * @see TransferTransitionalResultVisitor
 */
public abstract class SequentialMacroCommand<T> extends MacroCommand<T> implements TransferTransitionalResultVisitor {
    protected SequentialMacroCommand(ActionExecutor actionExecutor) {
        super(actionExecutor);
    }

    /**
     * To add the command to the commands nest
     *
     * @param command the instance to add
     * @param <N>     the result type of the nested command
     * @see RootCommand
     */
    @Override
    public <N> boolean putToNest(final NestedCommand<N> command) {
        return super.putToNest(wrap(command));
    }

    /**
     * To prepare command for sequential macro-command
     *
     * @param command nested command to wrap
     * @return wrapped nested command
     * @see SequentialMacroCommand#putToNest(NestedCommand)
     */
    public abstract NestedCommand<?> wrap(final NestedCommand<?> command);

    /**
     * To run macro-command's nested contexts<BR/>
     * Executing sequence of nested command contexts
     *
     * @param contexts nested command contexts deque to do
     * @param listener listener of the nested command context state-change
     * @return nested contexts after execution
     * @see Context.State#DONE
     * @see Context.State#FAIL
     * @see Context.State#CANCEL
     * @see SequentialMacroCommand#cancelSequentialNestedCommandContext(Context, Context.StateChangedListener)
     * @see SequentialMacroCommand#doSequentialNestedCommand(Context, AtomicReference, Context.StateChangedListener, AtomicBoolean)
     * @see AtomicReference
     * @see AtomicBoolean
     * @see Deque#forEach(Consumer)
     */
    @Override
    public Deque<Context<?>> executeNested(final Deque<Context<?>> contexts, final Context.StateChangedListener listener) {
        if (ObjectUtils.isEmpty(contexts)) {
            getLog().warn("Nothing to do");
            return contexts;
        }
        // flag if something went wrong previously
        final AtomicBoolean isPreviousFailed = new AtomicBoolean(false);
        // the value of previous nested command context
        final AtomicReference<Context<?>> previous = new AtomicReference<>(null);
        // sequential walking through contexts set
        return contexts.stream().map(context -> isPreviousFailed.get()
                // previous nested command's context.state has value FAIL? Cancel it
                ? cancelSequentialNestedCommandContext(context, listener)
                // try to execute nested command
                : doSequentialNestedCommand(context, previous, listener, isPreviousFailed)
        ).collect(Collectors.toCollection(LinkedList::new));
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

    // private methods
    // To mark canceled sequential nested command execution
    private <N> Context<N> cancelSequentialNestedCommandContext(final Context<N> context, final Context.StateChangedListener listener) {
        // getting last state from the context history and use it for the listener's notification
        final Context.State lastState = context.getHistory().states().getLast();
        getLog().debug("Cancel nested command execution from state {}", lastState);
        // update context-state-change listener
        listener.stateChanged(context, lastState, Context.State.CANCEL);
        // update context state to CANCEL
        context.setState(Context.State.CANCEL);
        return context;
    }

    // To do sequential nested command execution
    private <N> Context<N> doSequentialNestedCommand(final Context<N> current,
                                                     final AtomicReference<Context<?>> previousHolder,
                                                     final Context.StateChangedListener listener,
                                                     final AtomicBoolean isExecutionFailed) {
        final Context<?> previous = previousHolder.get();
        if (Objects.nonNull(previous)) {
            // transfer previous command's execution result to the current context input (RedoParameter)
            getLog().debug("Transferring the result to nested context of '{}'", current.getCommand().getId());
            // transferring data from previous context to current one
            transferringPreviousResult(previous, current);
        }

        // current nested command do executing
        getLog().debug("Doing nested command for {}", current);
        final Context<N> result = executeDoNested(current, listener);

        // checking nested command result after execution
        if (result.isDone()) {
            getLog().debug("Nested Command is executed well");
            // current nested command execution is successful
            previousHolder.getAndSet(result);
        } else {
            // current nested command execution is failed
            getLog().warn("Something went wrong with {}", result);
            isExecutionFailed.compareAndSet(false, true);
        }
        // returning nested command's execution result
        return result;
    }

    // To transfer result of previous nested context to current one
    private <N, S> void transferringPreviousResult(final Context<S> previous, final Context<N> current) {
        final Optional<RootCommand<S>> source = getSourceCommand(previous);
        if (source.isEmpty()) {
            // invalid previous context
            return;
        }
        // getting previously executed nested command id
        final String sourceCmdId = source.get().getId();
        // getting the result value to transfer
        final S value = previous.getResult().orElseThrow(() -> new CannotTransferCommandResultException(sourceCmdId));

        // everything is ready for transfer
        final String currentCmdId = current.getCommand().getId();
        getLog().debug("Transferring from '{}' to '{}' value:[{}]", sourceCmdId, currentCmdId, value);

        // transferring the result of previous nested command execution to current context
        if (source.get() instanceof CommandInSequence commandInSequence) {
            // transferring the value of previously executed nested command to current context redo-parameter
            commandInSequence.transferResultTo(this, value, current);
            getLog().debug("Transferred from '{}' to '{}' value:[{}] successfully", sourceCmdId, currentCmdId, value);
        } else {
            getLog().warn("Transferring from '{}' is impossible", sourceCmdId);
        }
    }

    // to find source command for the context
    private <S> Optional<RootCommand<S>> getSourceCommand(final Context<S> previous) {
        // check source context's result
        if (!previous.isDone()) {
            getLog().warn("Wrong state of previous context: {}", previous.getState());
            return Optional.empty();
        }
        final Optional<S> result = previous.getResult();
        if (ObjectUtils.isEmpty(result)) {
            getLog().warn("Wrong result of previous command: {}", result);
            return Optional.empty();
        }
        final RootCommand<S> source = previous.getCommand();
        if (isNull(source)) {
            // empty command instance, very strange case!
            getLog().error("Wrong context of previous command: {}", previous);
            return Optional.empty();
        }
        return Optional.of(source);
    }

    // to rollback nested command execution results
    private Context<?> rollbackNestedCommand(final Context<?> context, final AtomicBoolean isNestedRollbackFailed) {
        if (isNestedRollbackFailed.get()) {
            getLog().debug("Skipping nested undone because previous context failed");
            return context;
        }

        getLog().debug("Rolling back nested command for '{}'", context.getCommand().getId());
        // rollback nested command
        final Context<?> result = executeUndoNested(context);

        // check rollback result
        final String commandId = result.getCommand().getId();
        if (result.isFailed()) {
            // nested command rollback is failed
            getLog().warn("= Rollback of the nested command '{}' is failed", commandId, result.getException());
            isNestedRollbackFailed.compareAndSet(false, true);
        } else {
            // nested command rollback is successful
            getLog().debug("Rollback of the nested command '{}' is successful", commandId);
        }
        return result;
    }

    // inner class-wrapper for nested command

    /**
     * class-wrapper for chained nested commands in sequence
     */
    public abstract static class Chained<C extends RootCommand<?>> implements CommandInSequence {
        /**
         * To unwrap nested command
         *
         * @return unwrapped instance of the command
         */
        public abstract C unWrap();
    }
}
