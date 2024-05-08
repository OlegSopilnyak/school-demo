package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.base.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Command-Base: macro-command the command with nested commands inside
 */
public abstract class MacroCommand implements CompositeCommand {
    private final List<SchoolCommand> commands = new LinkedList<>();

    /**
     * To get the collection of commands used it composite
     *
     * @return collection of included commands
     */
    @Override
    public Collection<SchoolCommand> commands() {
        synchronized (commands) {
            return Collections.unmodifiableList(commands);
        }
    }

    /**
     * To add the command
     *
     * @param command the instance to add
     * @see SchoolCommand
     */
    @Override
    public void add(final SchoolCommand command) {
        synchronized (commands) {
            commands.add(command);
        }
    }

    /**
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     */
    @Override
    public <T> void executeDo(final Context<T> context) {
        final Object input = context.getRedoParameter();
        getLog().debug("Do redo for {}", input);
        try {
            final CommandParameterWrapper<T> wrapper = commandParameter(input);

            final ContextDeque<Context<T>> doneContexts = new ContextDeque<>(new LinkedList<>());
            final ContextDeque<Context<T>> failedContexts = new ContextDeque<>(new LinkedList<>());

            final Deque<Context<T>> nested = wrapper.getNestedContexts();

            final CountDownLatch executed = new CountDownLatch(nested.size());
            final Context.StateChangedListener<T> listener = (ctx, previous, newOne) -> {
                switch (newOne) {
                    case INIT, READY, WORK, UNDONE ->
                            getLog().debug("Changed state of '{}' form {} to {}", ctx.getCommand().getId(), previous, newOne);
                    case CANCEL -> executed.countDown();
                    case DONE -> {
                        addIntoContexts(ctx, doneContexts);
                        executed.countDown();
                    }
                    case FAIL -> {
                        addIntoContexts(ctx, failedContexts);
                        executed.countDown();
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + newOne);
                }
            };
            // run redo for nested contexts
            redoNestedContexts(nested, listener);
            // wait for all commands finished
            executed.await();
            // after run, done and fail dequeues processing
            afterRedoSet(context, doneContexts.deque, failedContexts.deque, nested);
        } catch (InterruptedException e) {
            getLog().error("Cannot wait finished '{}' with input {}", getId(), input, e);
            context.failed(e);
            /* Clean up whatever needs to be handled before interrupting  */
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            getLog().error("Cannot do redo of '{}' with input {}", getId(), input, e);
            context.failed(e);
        }
    }

    /**
     * To run redo for each macro-command's nested context
     *
     * @param nestedContexts nested contexts collection
     * @param listener       listener of context-state-change
     * @see this#redoNestedCommand(Context, Context.StateChangedListener)
     * @see Deque
     * @see java.util.LinkedList
     * @see Context
     * @see Context.StateChangedListener
     */
    protected <T> void redoNestedContexts(final Deque<Context<T>> nestedContexts, final Context.StateChangedListener<T> listener) {
        nestedContexts.forEach(ctx -> redoNestedCommand(ctx, listener));
    }

    /**
     * To execute one nested command
     *
     * @param nestedContext nested command execution context
     * @param listener      the lister of command state change
     */
    protected <P> Context<P> redoNestedCommand(final Context<P> nestedContext, final Context.StateChangedListener<P> listener) {
        nestedContext.addStateListener(listener);
        final SchoolCommand command = nestedContext.getCommand();
        try {
            command.doCommand(nestedContext);
        } catch (Exception e) {
            getLog().error("Cannot run redo for command:'{}'", command.getId(), e);
            nestedContext.failed(e);
        } finally {
            nestedContext.removeStateListener(listener);
        }
        return nestedContext;
    }

    /**
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    @Override
    public <T> void executeUndo(final Context<T> context) {
        final Object parameter = context.getUndoParameter();
        getLog().debug("Do undo for {}", parameter);
        try {
            final Deque<Context<T>> done = commandParameter(parameter);
            rollbackNestedDoneContexts(done);
            final Optional<Context<T>> fail = done.stream().filter(Context::isFailed).findFirst();
            if (fail.isPresent()) {
                throw fail.get().getException();
            }
            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            getLog().error("Cannot run redo for command:'{}'", getId(), e);
            context.failed(e);
        }
    }

    /**
     * To rollback changes for contexts with state DONE
     *
     * @param nestedContexts collection of nested contexts with DONE state
     * @see Context.State#DONE
     */
    protected <T> Deque<Context<T>> rollbackNestedDoneContexts(final Deque<Context<T>> nestedContexts) {
        return nestedContexts.stream()
                .map(ctx -> rollbackDoneContext(ctx.getCommand(), ctx))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * To rollback changes for contexts with state DONE
     *
     * @param nestedCommand nested command to do undo with nested context
     * @param nestedContext nested context with DONE state
     * @see SchoolCommand#undoCommand(Context)
     * @see Context.State#DONE
     * @see Context.State#FAIL
     */
    protected <T> Context<T> rollbackDoneContext(SchoolCommand nestedCommand, final Context<T> nestedContext) {
        try {
            nestedCommand.undoCommand(nestedContext);
        } catch (Exception e) {
            getLog().error("Cannot rollback for {}", nestedContext, e);
            nestedContext.failed(e);
        }
        return nestedContext;
    }

    // private methods
    private <T> void afterRedoSet(final Context<T> context,
                                  final Deque<Context<T>> done,
                                  final Deque<Context<T>> failed,
                                  final Deque<Context<T>> nested) {
        // check failed contexts
        if (failed.isEmpty()) {
            // no failed contexts
            context.setUndoParameter(done);
            context.setResult(nested.getLast().getResult().orElseThrow());
        } else {
            // exists failed contexts
            context.failed(failed.getFirst().getException());
            // rollback all done contexts
            rollbackNestedDoneContexts(done);
        }
    }

    private static <T> void addIntoContexts(final Context<T> context, final ContextDeque<Context<T>> contexts) {
        contexts.lock.lock();
        try {
            contexts.deque.add(context);
        } finally {
            contexts.lock.unlock();
        }
    }

    private static class ContextDeque<T> {
        private final Deque<T> deque;
        private final Lock lock = new ReentrantLock();

        private ContextDeque(Deque<T> deque) {
            this.deque = deque;
        }
    }
}
