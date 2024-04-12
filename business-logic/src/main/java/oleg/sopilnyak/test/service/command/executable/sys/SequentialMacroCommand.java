package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/**
 * MacroCommand-Base: macro-command the command with nested commands inside, uses sequence of command
 */
public abstract class SequentialMacroCommand<T> extends MacroCommand<T> {
    /**
     * To run macro-command's nested contexts<BR/>
     * Executing sequence of nested command contexts
     *
     * @param nestedContexts nested command contexts collection
     * @param listener       listener of context-state-change
     * @see super#redoNestedCommand(Context, Context.StateChangedListener)
     * @see Deque
     * @see java.util.LinkedList
     * @see Context
     * @see Context.StateChangedListener
     * @see Context.State#FAIL
     * @see Context.State#CANCEL
     */
    @Override
    protected void redoNested(Deque<Context> nestedContexts, Context.StateChangedListener listener) {
        final AtomicBoolean failed = new AtomicBoolean(false);
        final AtomicReference<Context> previousContext = new AtomicReference<>(null);
        nestedContexts.forEach(context -> {
            if (failed.get()) {
                // previous command's redo context.state had Context.State.FAIL
                context.addStateListener(listener);
                context.setState(Context.State.CANCEL);
                context.removeStateListener(listener);
            } else {
                // transfer previous context result to current one
                configureCurrentDoInput(previousContext.get(), context);
                // current redo context executing
                final Context current = redoNestedCommand(context, listener);
                final boolean isGoodResult = current.getState()  == Context.State.DONE;
                if (isGoodResult) previousContext.getAndSet(current);
                failed.compareAndSet(false, !isGoodResult);
            }
        });
    }

    /**
     * To transfer result form previous command to current context
     *
     * @param previousCommand previous successfully executed command
     * @param previousResult the result of previous command execution
     * @param targetContext current command context to execute
     */
    protected void transferPreviousRedoResult(SchoolCommand previousCommand, Optional previousResult, Context targetContext) {
    }

    /**
     * To rollback changes for contexts with state DONE<BR/>
     * sequential revers order of commands deque
     *
     * @param nestedContexts collection of contexts with DONE state
     * @see Context.State#DONE
     */
    @Override
    protected Deque<Context> rollbackNestedDoneContexts(Deque<Context> nestedContexts) {
        final List<Context> reverted = nestedContexts.stream().toList();
        Collections.reverse(reverted);
        return reverted.stream().map(this::rollbackDoneContext).collect(Collectors.toCollection(LinkedList::new));
    }

    // private methods
    private void configureCurrentDoInput(Context source, Context target) {
        if (isNull(source) || source.getState() != Context.State.DONE) return;
        final SchoolCommand<?> sourceCommand = source.getCommand();
        if (isNull(sourceCommand)) return;
        final Optional sourceCommandResult = source.getResult();
        if (isNull(sourceCommandResult) || sourceCommandResult.isEmpty()) return;
        getLog().debug("Transfer from '{}' result {} to '{}'",
                sourceCommand.getId(),
                sourceCommandResult,
                target.getCommand().getId());
        transferPreviousRedoResult(sourceCommand, sourceCommandResult, target);
    }
}
