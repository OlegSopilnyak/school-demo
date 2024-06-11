package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Command-Base: macro-command the command with nested commands inside
 *
 * @see RootCommand
 */
public abstract class MacroCommand<C extends RootCommand>
        implements CompositeCommand<C>, NestedCommandExecutionVisitor {
    // the list of nested commands
    private final List<C> netsedCommandsList = new LinkedList<>();

    /**
     * To get the collection of commands used into composite
     *
     * @return collection of nested commands
     */
    @Override
    public Collection<C> fromNest() {
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
    public void addToNest(final C command) {
        synchronized (netsedCommandsList) {
            netsedCommandsList.add(command);
        }
    }

    /**
     * To execute command redo with correct context state
     *
     * @param doContext context of redo execution
     * @param <T>       type of command execution result
     * @see Context
     * @see Context#getRedoParameter()
     * @see Context.State#WORK
     */
    @Override
    public <T> void executeDo(final Context<T> doContext) {
        final Object input = doContext.getRedoParameter();
        getLog().debug("Do redo for {}", input);
        try {
            final MacroCommandParameter<T> wrapper = commandParameter(input);

            final ContextDeque<Context<T>> doneContextsDeque = new ContextDeque<>();
            final ContextDeque<Context<T>> failedContextsDeque = new ContextDeque<>();

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
     * @param <T>           type of command execution result
     * @see RootCommand#doAsNestedCommand(NestedCommandExecutionVisitor, Context, Context.StateChangedListener)
     * @see Deque
     * @see Context.StateChangedListener
     */
    public <T> void doNestedCommands(final Deque<Context<T>> doContexts,
                                     final Context.StateChangedListener<T> stateListener) {
        doContexts.forEach(context -> {
            final RootCommand nestedCommand = context.getCommand();
            nestedCommand.doAsNestedCommand(this, context, stateListener);
        });
    }

    /**
     * To rollback command's execution with correct context state
     *
     * @param undoContext context of redo execution
     * @param <T>         type of command execution result
     * @see Context
     * @see Context#getUndoParameter()
     */
    @Override
    public <T> void executeUndo(final Context<T> undoContext) {
        final Object parameter = undoContext.getUndoParameter();
        getLog().debug("Do undo for {}", parameter);
        try {
            final Deque<Context<T>> successfulDoContexts = commandParameter(parameter);
            // rolling back successful nested commands
            rollbackDoneContexts(successfulDoContexts);
            // after rollback process check the contexts' states
            afterRollbackDoneCheck(successfulDoContexts);
            // rollback successful
            undoContext.setState(Context.State.UNDONE);
        } catch (Exception e) {
            getLog().error("Cannot run undo for command:'{}'", getId(), e);
            undoContext.failed(e);
        }
    }

    /**
     * To rollback changes for nested successful commands (contexts with state DONE)
     *
     * @param successfulDoContexts collection of nested contexts with DONE state
     * @param <T>                  type of command execution result
     * @see RootCommand#undoAsNestedCommand(NestedCommandExecutionVisitor, Context)
     * @see MacroCommand#undoNestedCommand(RootCommand, Context)
     * @see Context.State#DONE
     */
    protected <T> Deque<Context<T>> rollbackDoneContexts(final Deque<Context<T>> successfulDoContexts) {
        return successfulDoContexts.stream()
                .map(context -> {
                    final RootCommand nestedCommand = context.getCommand();
                    return nestedCommand.undoAsNestedCommand(this, context);
                })
                .collect(Collectors.toCollection(LinkedList::new));
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
        final Optional<Context<T>> failContext = doneContexts.stream().filter(Context::isFailed).findFirst();
        if (failContext.isPresent()) {
            throw failContext.get().getException();
        }
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
