package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import oleg.sopilnyak.test.service.command.type.composite.TransferResultVisitor;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/**
 * Sequential MacroCommand: macro-command the command with nested commands inside, uses sequence of command
 */
public abstract class SequentialMacroCommand extends MacroCommand<SchoolCommand> implements TransferResultVisitor {
    /**
     * To run macro-command's nested contexts<BR/>
     * Executing sequence of nested command contexts
     *
     * @param doContexts    nested command contexts collection
     * @param stateListener listener of context-state-change
     * @see super#doNestedCommand(Context, Context.StateChangedListener)
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
        final AtomicBoolean failed = new AtomicBoolean(false);
        final AtomicReference<Context<T>> previousContext = new AtomicReference<>(null);
        // sequential walking through contexts set
        doContexts.forEach(currentContext -> {
            if (failed.get()) {
                // previous command's redo context.state had Context.State.FAIL
                currentContext.addStateListener(stateListener);
                currentContext.setState(Context.State.CANCEL);
                currentContext.removeStateListener(stateListener);
            } else {
                // transfer previous context result to current one
                transferPreviousResultToCurrentContextRedoParameter(previousContext.get(), currentContext);
                // current redo context executing
                final Context<T> afterRedo = doNestedCommand(currentContext, stateListener);
                if (afterRedo.isDone()) {
                    // command do successful
                    previousContext.getAndSet(afterRedo);
                } else {
                    // command do failed
                    failed.compareAndSet(false, true);
                }
            }
        });
    }

    /**
     * To rollback changes for contexts with state DONE<BR/>
     * sequential revers order of commands deque
     *
     * @param undoContexts collection of contexts with DONE state
     * @see Deque
     * @see Context.State#DONE
     */
    @Override
    protected <T> Deque<Context<T>> rollbackDoneContexts(Deque<Context<T>> undoContexts) {
        final List<Context<T>> reverted = new ArrayList<>(undoContexts);
        // revert the order of undo contexts
        Collections.reverse(reverted);
        // rollback commands' changes
        return reverted.stream()
                .map(doneContext -> rollbackDoneContext(doneContext.getCommand(), doneContext))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    // private methods
    private <S, T> void transferPreviousResultToCurrentContextRedoParameter(
            final Context<S> source, final Context<T> target
    ) {
        if (isNull(source) || !source.isDone()) return;
        final SchoolCommand sourceCommand = source.getCommand();
        if (isNull(sourceCommand)) return;
        final Optional<S> result = source.getResult();
        if (isNull(result) || result.isEmpty()) return;

        // everything is ready for transfer
        getLog().debug(
                "Transfer from '{}' result {} to '{}'",
                sourceCommand.getId(), result, target.getCommand().getId()
        );

        // transferring result to target
        sourceCommand.transferResultTo(this, result.get(), target);
    }
}
