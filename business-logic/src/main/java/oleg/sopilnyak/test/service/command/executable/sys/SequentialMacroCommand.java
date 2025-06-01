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
     * @see SequentialMacroCommand#cancelSequentialNestedCommandContext(Context.StateChangedListener, Context)
     * @see SequentialMacroCommand#doSequentialNestedCommandContext(Context.StateChangedListener, Context, AtomicReference, AtomicBoolean)
     * @see AtomicReference
     * @see AtomicBoolean
     * @see Deque#forEach(Consumer)
     */
    @Override
    public void executeNested(final Deque<Context<?>> contexts, final Context.StateChangedListener listener) {
        // flag if something went wrong
        final AtomicBoolean isExecutionFailed = new AtomicBoolean(false);
        // the value of previous nested command context
        final AtomicReference<Context<?>> previousContextReference = new AtomicReference<>(null);

        // sequential walking through contexts set
        contexts.forEach(nestedContext -> {
            if (isExecutionFailed.get()) {
                // previous command's redo context.state had Context.State.FAIL
                cancelSequentialNestedCommandContext(listener, nestedContext);
            } else {
                doSequentialNestedCommandContext(listener, nestedContext, previousContextReference, isExecutionFailed);
            }
        });
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

    /**
     * To cancel sequential nested command execution
     *
     * @param stateListener listener of nested command context-state-change
     * @param context       nested command context
     * @see Context
     * @see Context.StateChangedListener
     * @see Context.State#FAIL
     * @see Context.State#CANCEL
     */
    private static void cancelSequentialNestedCommandContext(final Context.StateChangedListener stateListener,
                                                             final Context<?> context) {
        context.addStateListener(stateListener);
        context.setState(Context.State.CANCEL);
        context.removeStateListener(stateListener);
    }

    /**
     * To do sequential nested command execution
     *
     * @param stateListener            listener of nested command context-state-change
     * @param currentNestedContext     current nested command context
     * @param previousContextReference previous nested command context holder
     * @param isDoFailed               status of nested commands execution holder
     * @see Context
     * @see Context.StateChangedListener
     * @see RootCommand#doAsNestedCommand(NestedCommandExecutionVisitor, Context, Context.StateChangedListener)
     * @see SequentialMacroCommand#transferringPreviousResult(Context, Context)
     */
    private void doSequentialNestedCommandContext(final Context.StateChangedListener stateListener,
                                                  final Context<?> currentNestedContext,
                                                  final AtomicReference<Context<?>> previousContextReference,
                                                  final AtomicBoolean isDoFailed) {
        final RootCommand<?> command = currentNestedContext.getCommand();
        final Context<?> previousNestedContext = previousContextReference.get();
        if (Objects.nonNull(previousNestedContext)) {
            // transfer previous command's do result to current context
            getLog().debug("Transferring the result to nested context of '{}'", command.getId());
            transferringPreviousResult(previousNestedContext, currentNestedContext);
        }

        // current nested command do executing
        getLog().debug("Doing nested command for {}", currentNestedContext);
        command.doAsNestedCommand(this, currentNestedContext, stateListener);

        // checking nested command do result
        if (currentNestedContext.isDone()) {
            // current nested command's do is successful
            previousContextReference.getAndSet(currentNestedContext);
        } else {
            // current nested command's do is failed
            getLog().warn("Something went wrong with {}", currentNestedContext);
            isDoFailed.compareAndSet(false, true);
        }
    }

    /**
     * To transfer result of previous nested context to current one
     *
     * @param previous the instance of previous nested command context
     * @param current  the instance of nested context before RootCommand#doAsNestedCommand(...)
     * @param <S>      the type of previous nested command context result
     * @see RootCommand#doAsNestedCommand(NestedCommandExecutionVisitor, Context, Context.StateChangedListener)
     * @see Context
     * @see Context#getResult()
     * @see Context#getCommand()
     * @see RootCommand#getId()
     * @see CannotTransferCommandResultException
     * @see NestedCommand.InSequence#transferResultTo(TransferResultVisitor, Object, Context)
     */
    private <S> void transferringPreviousResult(final Context<S> previous, final Context<?> current) {
        // check source context's result
        if (!previous.isDone()) {
            getLog().warn("Wrong state of previous context: {}", previous.getState());
            return;
        }
        final Optional<S> result = previous.getResult();
        if (ObjectUtils.isEmpty(result)) {
            getLog().warn("Wrong result of previous command: {}", result);
            return;
        }
        // check source context's command
        final RootCommand<S> sourceCommand = previous.getCommand();
        if (isNull(sourceCommand)) {
            // empty command instance, very strange case!
            getLog().error("Wrong context of previous command: {}", previous);
            return;
        }
        final String sourceCmdId = sourceCommand.getId();
        // getting result value to transfer
        final S resultValue = result.orElseThrow(() -> new CannotTransferCommandResultException(sourceCmdId));

        // everything is ready for transfer
        final String currentCmdId = current.getCommand().getId();
        getLog().debug("Transferring from '{}' to '{}' value:[{}]", sourceCmdId, currentCmdId, resultValue);

        // transferring result to current context
        if (sourceCommand instanceof NestedCommand.InSequence commandInSequence) {
            commandInSequence.transferResultTo(this, resultValue, current);
            getLog().debug("Transferred from '{}' to '{}' value:[{}] successfully", sourceCmdId, currentCmdId, resultValue);
        } else {
            getLog().warn("Transferring from '{}' is impossible", sourceCmdId);
        }
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
