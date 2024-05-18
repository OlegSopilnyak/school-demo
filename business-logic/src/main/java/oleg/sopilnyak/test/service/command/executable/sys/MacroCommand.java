package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.CompositeCommand;
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
public abstract class MacroCommand<C extends SchoolCommand> implements CompositeCommand<C> {
    private final List<C> commands = new LinkedList<>();

    /**
     * To get the collection of commands used it composite
     *
     * @return collection of included commands
     */
    @Override
    public Collection<C> commands() {
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
    public void add(final C command) {
        synchronized (commands) {
            commands.add(command);
        }
    }

    /**
     * To execute command redo with correct context state
     *
     * @param doContext context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     */
    @Override
    public <T> void executeDo(final Context<T> doContext) {
        final Object input = doContext.getRedoParameter();
        getLog().debug("Do redo for {}", input);
        try {
            final CommandParameterWrapper<T> wrapper = commandParameter(input);

            final ContextDeque<Context<T>> doneContextsDeque = new ContextDeque<>(new LinkedList<>());
            final ContextDeque<Context<T>> failedContextsDeque = new ContextDeque<>(new LinkedList<>());

            final Deque<Context<T>> nestedContexts = wrapper.getNestedContexts();

            final CountDownLatch executed = new CountDownLatch(nestedContexts.size());
            final Context.StateChangedListener<T> stateListener = (context, previous, newOne) -> {
                final String commandId = context.getCommand().getId();
                switch (newOne) {
                    case INIT, READY, WORK, UNDONE ->
                            getLog().debug("Changed state of '{}' from State:{} to :{}", commandId, previous, newOne);
                    case CANCEL -> {
                        getLog().debug("Command '{}' is Canceled from State:{}", commandId, previous);
                        executed.countDown();
                    }
                    case DONE -> {
                        getLog().debug("Command '{}' is Done from State:{}", commandId, previous);
                        doneContextsDeque.putToTail(context);
                        executed.countDown();
                    }
                    case FAIL -> {
                        getLog().debug("Command '{}' is Failed from State:{}", commandId, previous);
                        failedContextsDeque.putToTail(context);
                        executed.countDown();
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + newOne);
                }
            };
            // run redo for nested contexts
            doNestedCommands(nestedContexts, stateListener);
            // wait for all commands finished
            executed.await();
            // after run, done and fail dequeues processing
            afterDoneSetup(doContext, doneContextsDeque.deque, failedContextsDeque.deque, nestedContexts);
        } catch (InterruptedException e) {
            getLog().error("Cannot wait finished '{}' with input {}", getId(), input, e);
            doContext.failed(e);
            // Clean up whatever needs to be handled before interrupting
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            getLog().error("Cannot do redo of '{}' with input {}", getId(), input, e);
            doContext.failed(e);
        }
    }

    /**
     * To run redo for each macro-command's nested context
     *
     * @param doContexts    nested contexts collection
     * @param stateListener listener of context-state-change
     * @see MacroCommand#doNestedCommand(Context, Context.StateChangedListener)
     * @see Deque
     * @see java.util.LinkedList
     * @see Context
     * @see Context.StateChangedListener
     */
    public  <T> void doNestedCommands(final Deque<Context<T>> doContexts,
                                        final Context.StateChangedListener<T> stateListener) {
        doContexts.forEach(context -> doNestedCommand(context, stateListener));
    }

    /**
     * To execute one nested command
     *
     * @param doContext     nested command execution context
     * @param stateListener the lister of command state change
     */
    protected <T> Context<T> doNestedCommand(final Context<T> doContext,
                                             final Context.StateChangedListener<T> stateListener) {
        doContext.addStateListener(stateListener);
        final SchoolCommand command = doContext.getCommand();
        final String commandId = command.getId();
        try {
            command.doCommand(doContext);
            getLog().debug("Command:'{}' is done context:{}", commandId, doContext);
        } catch (Exception e) {
            getLog().error("Cannot run do for command:'{}'", commandId, e);
            doContext.failed(e);
        } finally {
            doContext.removeStateListener(stateListener);
        }
        return doContext;
    }

    /**
     * To rollback command's execution with correct context state
     *
     * @param undoContext context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    @Override
    public <T> void executeUndo(final Context<T> undoContext) {
        final Object parameter = undoContext.getUndoParameter();
        getLog().debug("Do undo for {}", parameter);
        try {
            final Deque<Context<T>> doneContexts = commandParameter(parameter);
            // rolling back successful commands
            rollbackDoneContexts(doneContexts);
            // after rollback process check
            afterRollbackDoneCheck(doneContexts);
            // rollback successful
            undoContext.setState(Context.State.UNDONE);
        } catch (Exception e) {
            getLog().error("Cannot run undo for command:'{}'", getId(), e);
            undoContext.failed(e);
        }
    }

    /**
     * To rollback changes for contexts with state DONE
     *
     * @param undoContexts collection of nested contexts with DONE state
     * @see Context.State#DONE
     */
    protected <T> Deque<Context<T>> rollbackDoneContexts(final Deque<Context<T>> undoContexts) {
        return undoContexts.stream()
                .map(context -> rollbackDoneContext(context.getCommand(), context))
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * To rollback changes for contexts with state DONE
     *
     * @param nestedCommand nested command to do undo with nested context (could be Override)
     * @param undoContext   nested context with DONE state
     * @see SchoolCommand#undoCommand(Context)
     * @see Context.State#DONE
     * @see Context.State#FAIL
     */
    protected <T> Context<T> rollbackDoneContext(final SchoolCommand nestedCommand,
                                                 final Context<T> undoContext) {
        try {
            nestedCommand.undoCommand(undoContext);
            getLog().debug("Rolled back done command '{}' with context:{}", nestedCommand.getId(), undoContext);
        } catch (Exception e) {
            getLog().error("Cannot rollback for {}", undoContext, e);
            undoContext.failed(e);
        }
        return undoContext;
    }

    // private methods
    private <T> void afterDoneSetup(final Context<T> doneContext,
                                    final Deque<Context<T>> done,
                                    final Deque<Context<T>> failed,
                                    final Deque<Context<T>> nested) {
        // check failed contexts
        if (failed.isEmpty()) {
            // no failed contexts
            doneContext.setUndoParameter(done);
            doneContext.setResult(nested.getLast().getResult().orElseThrow());
        } else {
            // found failed contexts, collect the exception
            doneContext.failed(failed.getFirst().getException());
            // rollback all successful contexts
            getLog().warn("Rolling back all successful commands {}", done);
            rollbackDoneContexts(done);
        }
    }

    private static <T> void afterRollbackDoneCheck(Deque<Context<T>> doneContexts) throws Exception {
        final Optional<Context<T>> fail = doneContexts.stream().filter(Context::isFailed).findFirst();
        if (fail.isPresent()) {
            throw fail.get().getException();
        }
    }

    /**
     * Synchronized Contexts Deque
     *
     * @param <T> type of deque item
     * @see Context
     */
    static final class ContextDeque<T> {
        private final Deque<T> deque;
        private final Lock locker = new ReentrantLock();

        private ContextDeque(Deque<T> deque) {
            this.deque = deque;
        }

        private void putToTail(final T context) {
            locker.lock();
            try {
                deque.addLast(context);
            } finally {
                locker.unlock();
            }
        }
    }
}
