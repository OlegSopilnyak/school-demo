package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.TransferResultVisitor;
import oleg.sopilnyak.test.service.exception.CannotTransferCommandResultException;
import org.springframework.lang.NonNull;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/**
 * Sequential MacroCommand: macro-command the command with nested commands inside, uses sequence of command
 */
public abstract class SequentialMacroCommand extends MacroCommand implements TransferResultVisitor {
    /**
     * To add the command to the commands nest
     *
     * @param command the instance to add
     * @see RootCommand
     */
    @Override
    public void addToNest(NestedCommand command) {
        super.addToNest(wrap(command));
    }

    /**
     * To prepare command for sequential macro-command
     *
     * @param command nested command to wrap
     * @return wrapped nested command
     * @see SequentialMacroCommand#addToNest(NestedCommand)
     */
    @NonNull
    public abstract NestedCommand wrap(final NestedCommand command);

    /**
     * To run macro-command's nested contexts<BR/>
     * Executing sequence of nested command contexts
     *
     * @param doContexts    nested command contexts collection
     * @param stateListener listener of context-state-change
     * @see RootCommand#doAsNestedCommand(NestedCommandExecutionVisitor, Context, Context.StateChangedListener)
     * @see Deque
     * @see java.util.LinkedList
     * @see Context
     * @see Context.StateChangedListener
     * @see Context.State#FAIL
     * @see Context.State#CANCEL
     */
    @Override
    public <T> void doNestedCommands(final Deque<Context<T>> doContexts,
                                     final Context.StateChangedListener<T> stateListener) {
        final AtomicBoolean isDoFailed = new AtomicBoolean(false);
        final AtomicReference<Context<T>> previousContextReference = new AtomicReference<>(null);
        // sequential walking through contexts set
        doContexts.forEach(current -> {
            if (isDoFailed.get()) {
                // previous command's redo context.state had Context.State.FAIL
                current.addStateListener(stateListener);
                current.setState(Context.State.CANCEL);
                current.removeStateListener(stateListener);
            } else {
                final RootCommand command = current.getCommand();
                final Context<T> previous = previousContextReference.get();
                if (Objects.nonNull(previous)) {
                    // transfer previous command's do result to current context
                    getLog().debug("Transferring the result");
                    transferringPreviousResult(previous, current);
                }
                // current nested command do executing
                getLog().debug("Doing nested command for {}", current);
                command.doAsNestedCommand(this, current, stateListener);
                if (current.isDone()) {
                    // current nested command's do is successful
                    previousContextReference.getAndSet(current);
                } else {
                    // current nested command's do is failed
                    isDoFailed.compareAndSet(false, true);
                }
            }
        });
    }

    /**
     * To rollback changes for nested contexts with state DONE<BR/>
     * sequential revers order of commands deque
     *
     * @param doneContexts collection of contexts with DONE state
     * @see Deque#stream()
     * @see AtomicBoolean
     * @see Collections#reverse(List)
     * @see SequentialMacroCommand#undoNested(Context, AtomicBoolean)
     * @see Context.State#UNDONE
     */
    @Override
    public <T> Deque<Context<T>> undoNestedCommands(Deque<Context<T>> doneContexts) {
        final List<Context<T>> reverted = new ArrayList<>(doneContexts);
        // revert the order of undo contexts
        Collections.reverse(reverted);
        final AtomicBoolean isFailed = new AtomicBoolean(false);
        // rollback commands' changes
        return reverted.stream()
                .map(nestedDoneContext -> undoNested(nestedDoneContext, isFailed))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    // private methods
    private <T> Context<T> undoNested(final Context<T> nestedDoneContext, final AtomicBoolean isFailed) {
        return isFailed.get() ? nestedDoneContext : doUndoNestedCommand(nestedDoneContext, isFailed);
    }

    private <T> Context<T> doUndoNestedCommand(final Context<T> nestedDoneContext, final AtomicBoolean isFailed) {
        final Context<T> undoContext = nestedDoneContext.getCommand().undoAsNestedCommand(this, nestedDoneContext);
        if (undoContext.isFailed()) {
            // nested command undo is failed
            isFailed.compareAndSet(false, true);
        }
        return undoContext;
    }

    private <S, T> void transferringPreviousResult(final Context<S> previous, final Context<T> current) {
        // check source context's result
        if (!previous.isDone()) return;
        final Optional<S> result = previous.getResult();
        if (ObjectUtils.isEmpty(result)) return;
        // check source context's command
        final RootCommand sourceCommand = previous.getCommand();
        if (isNull(sourceCommand)) return;
        // getting result value to transfer
        final S resultValue = result.orElseThrow(() -> new CannotTransferCommandResultException(sourceCommand.getId()));
        // everything is ready for transfer
        final String sourceCmdId = sourceCommand.getId();
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

}
