package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/**
 * Sequential MacroCommand: macro-command the command with nested commands inside, uses sequence of command
 */
public abstract class SequentialMacroCommand extends MacroCommand<SchoolCommand> {
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
    protected <T> void doNestedCommands(final Deque<Context<T>> doContexts,
                                        final Context.StateChangedListener<T> stateListener) {
        final AtomicBoolean failed = new AtomicBoolean(false);
        final AtomicReference<Context<T>> previousContext = new AtomicReference<>(null);
        // sequential walking through contexts set
        doContexts.forEach(doContext -> {
            if (failed.get()) {
                // previous command's redo context.state had Context.State.FAIL
                doContext.addStateListener(stateListener);
                doContext.setState(Context.State.CANCEL);
                doContext.removeStateListener(stateListener);
            } else {
                // transfer previous context result to current one
                configureCurrentDoParameter(previousContext.get(), doContext);
                // current redo context executing
                final Context<T> afterRedo = doNestedCommand(doContext, stateListener);
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
     * To transfer result form previous command to current context
     *
     * @param previousCommand previous successfully executed command
     * @param previousResult  the result of previous command execution
     * @param targetContext   current command context to execute command's redo
     * @param <S>             type of previous result
     * @param <T>             type of target result
     * @see SchoolCommand
     * @see Context
     * @see Optional
     */
    protected <S, T> void transferPreviousRedoResult(final SchoolCommand previousCommand,
                                                     final Optional<S> previousResult,
                                                     final Context<T> targetContext) {
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
    private <S, T> void configureCurrentDoParameter(Context<S> source, Context<T> target) {
        if (isNull(source) || !source.isDone()) return;
        final SchoolCommand sourceCommand = source.getCommand();
        if (isNull(sourceCommand)) return;
        final Optional<S> sourceResult = source.getResult();
        if (isNull(sourceResult) || sourceResult.isEmpty()) return;
        getLog().debug(
                "Transfer from '{}' result {} to '{}'",
                sourceCommand.getId(), sourceResult, target.getCommand().getId()
        );
        transferPreviousRedoResult(sourceCommand, sourceResult, target);
    }
}
