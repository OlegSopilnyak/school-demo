package oleg.sopilnyak.test.service.command.executable.sys;

import static java.util.Objects.isNull;

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
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.TransferResultVisitor;
import oleg.sopilnyak.test.service.exception.CannotTransferCommandResultException;
import org.springframework.util.ObjectUtils;

/**
 * Sequential MacroCommand: macro-command the command with nested commands inside, uses sequence of command
 *
 * @param <T> the type of command execution (do) result
 * @see MacroCommand
 */
public abstract class SequentialMacroCommand<T> extends MacroCommand<T> implements TransferResultVisitor {
    protected SequentialMacroCommand(ActionExecutor actionExecutor) {
        super(actionExecutor);
    }

    /**
     * To add the command to the commands nest
     *
     * @param command the instance to add
     * @see RootCommand
     */
    @Override
    public boolean putToNest(final NestedCommand<?> command) {
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
     * @see SequentialMacroCommand#cancelSequentialNestedCommandContext(Context, Context.StateChangedListener)
     * @see SequentialMacroCommand#doSequentialNestedCommand(Context, AtomicReference, Context.StateChangedListener, AtomicBoolean)
     * @see AtomicReference
     * @see AtomicBoolean
     * @see Deque#forEach(Consumer)
     */
    @Override
    public Deque<Context<?>> executeNested(final Deque<Context<?>> contexts, final Context.StateChangedListener listener) {
        // flag if something went wrong
        final AtomicBoolean isExecutionFailed = new AtomicBoolean(false);
        // the value of previous nested command context
        final AtomicReference<Context<?>> previousContextReference = new AtomicReference<>(null);
        // sequential walking through contexts set
        return contexts.stream().map(context -> isExecutionFailed.get()
                        ?
                        // previous nested command's context.state has value FAIL, cancel it
                        cancelSequentialNestedCommandContext(context, listener)
                        :
                        // try to execute nested command
                        doSequentialNestedCommand(context, previousContextReference, listener, isExecutionFailed)
                )
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * To mark canceled sequential nested command execution
     *
     * @param context  nested command context
     * @param listener listener of nested command context-state-change
     * @return canceled command-context
     * @see Context
     * @see Context.StateChangedListener
     * @see Context.State#CANCEL
     */
    private <N> Context<N> cancelSequentialNestedCommandContext(final Context<N> context, final Context.StateChangedListener listener) {
        // getting last state from the context history and use it for the listener's notification
        listener.stateChanged(context, context.getHistory().states().getLast(), Context.State.CANCEL);
        context.setState(Context.State.CANCEL);
        return context;
    }

    /**
     * To do sequential nested command execution
     *
     * @param <N>               the type of nested command execution result
     * @param current           current nested command context
     * @param previousHolder    previous nested command context holder
     * @param listener          listener of nested command's state change
     * @param isExecutionFailed status of nested commands execution holder
     * @see Context
     * @see Context#getCommand()
     * @see Context#getRedoParameter()
     * @see Context.StateChangedListener
     * @see RootCommand#doAsNestedCommand(NestedCommandExecutionVisitor, Context, Context.StateChangedListener)
     * @see SequentialMacroCommand#transferringPreviousResult(Context, Context)
     */
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

    /**
     * To transfer result of previous nested context to current one
     *
     * @param previous the instance of previous nested command context
     * @param current  the instance of nested context before RootCommand#doAsNestedCommand(...)
     * @param <N>               the type of nested command execution result
     * @param <S>      the type of previous nested command context result
     * @see RootCommand#doAsNestedCommand(NestedCommandExecutionVisitor, Context, Context.StateChangedListener)
     * @see Context
     * @see Context#getResult()
     * @see Context#getCommand()
     * @see RootCommand#getId()
     * @see CannotTransferCommandResultException
     * @see NestedCommand.InSequence#transferResultTo(TransferResultVisitor, Object, Context)
     */
    private <N,S> void transferringPreviousResult(final Context<S> previous, final Context<N> current) {
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
        if (source.get() instanceof NestedCommand.InSequence commandInSequence) {
            // transferring the value of previously executed nested command to current context redo-parameter
            commandInSequence.transferResultTo(this, value, current);
            getLog().debug("Transferred from '{}' to '{}' value:[{}] successfully", sourceCmdId, currentCmdId, value);
        } else {
            getLog().warn("Transferring from '{}' is impossible", sourceCmdId);
        }
    }

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


    /**
     * To rollback changes for nested contexts with state DONE
     * <BR/>sequential revers order of commands deque
     *
     * @param doneContexts collection of contexts with DONE state
     * @see Deque#stream()
     * @see AtomicBoolean
     * @see Collections#reverse(List)
     * @see SequentialMacroCommand#undoNested(Context, AtomicBoolean)
     * @see Context.State#UNDONE
     */
    @Override
    public Deque<Context<?>> rollbackNestedDone(Input<Deque<Context<?>>> doneContexts) {
        final List<Context<?>> reverted = new ArrayList<>(doneContexts.value());
        // revert the order of undo contexts
        Collections.reverse(reverted);
        final AtomicBoolean isUndoNestedFailed = new AtomicBoolean(false);
        // rollback commands' changes
        return reverted.stream()
                .map(nestedDoneContext -> undoNested(nestedDoneContext, isUndoNestedFailed))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    // private methods
    private Context<?> undoNested(final Context<?> nestedDoneContext, final AtomicBoolean isFailed) {
        return isFailed.get() ? nestedDoneContext : undoNestedCommand(nestedDoneContext, isFailed);
    }

    private Context<?> undoNestedCommand(final Context<?> nestedDoneContext, final AtomicBoolean isFailed) {
        final Context<?> nestedContext = nestedDoneContext.getCommand().undoAsNestedCommand(this, nestedDoneContext);
        if (nestedContext.isFailed()) {
            // nested command undo is failed
            isFailed.compareAndSet(false, true);
        }
        return nestedContext;
    }

    // inner class-wrapper for nested command

    /**
     * class-wrapper for chained nested commands in sequence
     */
    public abstract static class Chained<C extends RootCommand<?>> implements NestedCommand.InSequence {
        /**
         * To unwrap nested command
         *
         * @return unwrapped instance of the command
         */
        public abstract C unWrap();
    }
}
