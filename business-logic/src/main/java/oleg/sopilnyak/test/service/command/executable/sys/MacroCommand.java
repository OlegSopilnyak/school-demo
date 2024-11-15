package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

/**
 * Command-Base: macro-command the command with nest of commands inside
 *
 * @param <T> the type of command execution (do) result
 * @see RootCommand
 * @see CompositeCommand
 * @see NestedCommandExecutionVisitor
 */
public abstract class MacroCommand<T> implements CompositeCommand<T>, NestedCommandExecutionVisitor {
    private static final Logger log = LoggerFactory.getLogger(MacroCommand.class);
    // the list of nested commands
    private final List<NestedCommand<?>> netsedCommandsList = new LinkedList<>();

    /**
     * To get the collection of commands used into composite
     *
     * @return collection of nested commands
     */
    @Override
    public Collection<NestedCommand<?>> fromNest() {
        synchronized (netsedCommandsList) {
            return Collections.unmodifiableList(netsedCommandsList);
        }
    }

    /**
     * To add the command to the commands nest
     *
     * @param command the instance to add
     * @see RootCommand
     */
    @Override
    public void addToNest(final NestedCommand<?> command) {
        synchronized (netsedCommandsList) {
            netsedCommandsList.add(command);
        }
    }

    /**
     * To execute command redo with correct context state
     *
     * @param doContext context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     * @see MacroCommandParameter#getNestedContexts()
     * @see MacroCommand#doNestedCommands(Deque, Context.StateChangedListener)
     * @see CountDownLatch#await()
     * @see MacroCommand#getDoCommandResult(Deque)
     */
    @Override
    public void executeDo(final Context<T> doContext) {
        final Object input = doContext.getRedoParameter();
        getLog().debug("Do redo for {}", input);
        try {
            final Deque<Context<?>> nested = doContext.<MacroCommandParameter>getRedoParameter().getNestedContexts();
            final int nestedCount = nested.size();

            final ContextDeque<Context<?>> done = new ContextDeque<>();
            final Deque<Context<?>> successDeque = done.deque;
            final ContextDeque<Context<?>> failed = new ContextDeque<>();
            final Deque<Context<?>> failDeque = failed.deque;
            final CountDownLatch nestedCommandsExecuted = new CountDownLatch(nestedCount);

            // run nested command's do for nested contexts
            getLog().debug("Running {} nested commands 'doCommand'", nestedCount);
            doNestedCommands(nested, new NestedCommandStateChangedListener(done, failed, nestedCommandsExecuted));

            // wait for all nested commands have done
            getLog().debug("Waiting for {} nested commands done", nestedCount);
            nestedCommandsExecuted.await();

            // after run, success and fail dequeues processing
            afterDoneSetup(doContext, successDeque, failDeque, nested);
        } catch (InterruptedException e) {
            getLog().error("Could not wait do finished '{}' with input {}", getId(), input, e);
            doContext.failed(e);
            // Clean up whatever needs to be handled before interrupting
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            getLog().error("Cannot do Do of '{}' with input {}", getId(), input, e);
            doContext.failed(e);
        }
    }

    /**
     * To run redo for each macro-command's nested context
     * <BR/>sequential traversal of nested contexts
     *
     * @param doContexts    nested contexts collection
     * @param stateListener listener of context-state-change
     * @see Context
     * @see Deque
     * @see Context.StateChangedListener
     * @see RootCommand#doAsNestedCommand(NestedCommandExecutionVisitor, Context, Context.StateChangedListener<?>)
     */
    public void doNestedCommands(final Deque<Context<?>> doContexts, final Context.StateChangedListener stateListener) {
        doContexts.forEach(nestedContext ->
            nestedContext.getCommand().doAsNestedCommand(this, nestedContext, stateListener)
        );
    }

    /**
     * To rollback command's execution with correct context state (DONE)
     * <BR/> the type of command result doesn't matter
     *
     * @param undoContext context of redo execution
     * @see Context
     * @see Context.State#DONE
     * @see Context#getUndoParameter()
     * @see MacroCommand#undoNestedCommands(Deque)
     * @see MacroCommand#afterRollbackDoneCheck(Context)
     */
    @Override
    public void executeUndo(final Context<?> undoContext) {
        final Object parameter = undoContext.getUndoParameter();
        getLog().debug("Do undo for {}", parameter);
        try {
            // rolling back successful nested do command contexts
            undoNestedCommands(undoContext.getUndoParameter());
            // after rollback process, check the contexts' states
            afterRollbackDoneCheck(undoContext);
        } catch (Exception e) {
            getLog().error("Cannot run undo for command:'{}'", getId(), e);
            undoContext.failed(e);
        }
    }

    /**
     * To rollback changes for nested successful commands (contexts with state DONE)
     *
     * @param doneContexts collection of nested contexts with DONE state
     * @see NestedCommand#undoAsNestedCommand(NestedCommandExecutionVisitor, Context)
     * @see MacroCommand#undoNestedCommand(RootCommand, Context)
     * @see Context.State#DONE
     */
    public Deque<Context<?>> undoNestedCommands(final Deque<Context<?>> doneContexts) {
        return doneContexts.stream().filter(Context::isDone).map(context -> {
            final NestedCommand<?> command = context.getCommand();
            return command.undoAsNestedCommand(this, context);
        }).collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * To get main do-command result from nested command-contexts
     *
     * @param nested nested command-contexts
     * @return the command result's value
     */
    @SuppressWarnings("unchecked")
    public T getDoCommandResult(final Deque<Context<?>> nested) {
        return (T) nested.getLast().getResult().orElseThrow();
    }

    // private methods
    private void afterDoneSetup(final Context<T> doneContext,
                                final Deque<Context<?>> done,
                                final Deque<Context<?>> failed,
                                final Deque<Context<?>> nested) {
        // check failed contexts
        if (ObjectUtils.isEmpty(failed)) {
            // no failed contexts
            final T doResult = getDoCommandResult(nested);
            getLog().debug("Command executed well and returned {}", doResult);
            doneContext.setUndoParameter(done);
            doneContext.setResult(doResult);
        } else {
            // found failed contexts, collect the exception
            doneContext.failed(failed.getFirst().getException());
            // rollback all successful contexts
            getLog().warn("Rolling back all successful commands {}", done);
            undoNestedCommands(done);
        }
    }

    private <N> void afterRollbackDoneCheck(Context<?> undoContext) {
        final Deque<Context<N>> undoneContexts = undoContext.getUndoParameter();
        final Context<?> failedContext = undoneContexts.stream().filter(Context::isFailed).findFirst().orElse(null);

        if (isNull(failedContext)) {
            // no errors found
            log.debug("No errors found after undo is done");
            undoContext.setState(Context.State.UNDONE);
            return;
        }
        // something went wrong during undo nested commands
        undoContext.failed(failedContext.getException());
        // rolling back nested undo changes calling nested.doAsNestedCommand(...)
        log.debug("Rolling back from undone {} command(s)", undoneContexts.size());
        final String logTemplate = "Changed state of '{}' from State:{} to :{}";
        final Context.StateChangedListener stateListener = (c, p, n) -> log.debug(logTemplate, getId(), p, n);
        // to rollback undone nested contexts
        undoneContexts.stream().filter(Context::isUndone).forEach(context -> {
            final NestedCommand<N> command = context.getCommand();
            context.setState(Context.State.READY);
            command.doAsNestedCommand(this, context, stateListener);
        });
    }

    /**
     * Synchronized Contexts Deque
     *
     * @param <T> type of deque item
     * @see Deque#addLast(Object)
     * @see Lock#lock()
     * @see Lock#unlock()
     */
    static final class ContextDeque<T> {
        private final Deque<T> deque = new LinkedList<>();
        private final Lock locker = new ReentrantLock();

        void putToTail(final T context) {
            locker.lock();
            try {
                deque.addLast(context);
            } finally {
                locker.unlock();
            }
        }
    }

    private class NestedCommandStateChangedListener implements Context.StateChangedListener {
        final ContextDeque<Context<?>> doneContextsDeque;
        final ContextDeque<Context<?>> failedContextsDeque;
        final CountDownLatch executed;

        private NestedCommandStateChangedListener(final ContextDeque<Context<?>> doneContextsDeque,
                                                  final ContextDeque<Context<?>> failedContextsDeque,
                                                  final CountDownLatch executed) {
            this.doneContextsDeque = doneContextsDeque;
            this.failedContextsDeque = failedContextsDeque;
            this.executed = executed;
        }

        @Override
        public void stateChanged(Context<?> context, Context.State previous, Context.State newOne) {
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
        }
    }

}
