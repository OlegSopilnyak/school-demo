package oleg.sopilnyak.test.service.command.executable.sys;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import org.springframework.util.ObjectUtils;

/**
 * Command-Base: macro-command the command with nest of commands inside
 *
 * @param <T> the type of command execution (do) result
 * @see RootCommand
 * @see CompositeCommand
 * @see NestedCommandExecutionVisitor
 */
public abstract class MacroCommand<T> implements CompositeCommand<T>, NestedCommandExecutionVisitor {
    // the list of nested commands
    private final List<NestedCommand<?>> netsedCommandsList = new LinkedList<>();

    /**
     * To get the collection of nested commands, used in the composite
     *
     * @return collection of nested commands
     */
    @Override
    public Collection<NestedCommand<?>> fromNest() {
        synchronized (netsedCommandsList) {
            return List.copyOf(netsedCommandsList);
        }
    }

    /**
     * To add the command to the commands nest
     *
     * @param command the instance to add
     * @see NestedCommand
     */
    @Override
    public boolean putToNest(final NestedCommand<?> command) {
        synchronized (netsedCommandsList) {
            return netsedCommandsList.add(command);
        }
    }

    /**
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see MacroCommandParameter#getNestedContexts()
     * @see NestedCommandStateChangedListener
     * @see MacroCommand#executeNested(Deque, Context.StateChangedListener)
     * @see CountDownLatch#await()
     * @see MacroCommand#postExecutionProcessing(Context, Deque, Deque, Deque)
     */
    @Override
    public void executeDo(final Context<T> context) {
        final Input<MacroCommandParameter> inputParameter = context.getRedoParameter();
        try {
            checkNullParameter(inputParameter);
            final MacroCommandParameter mainCommandParameter = inputParameter.value();
            checkNullParameter(mainCommandParameter);
            getLog().debug("Do Execution For {}", mainCommandParameter);
            final Deque<Context<?>> nested = mainCommandParameter.getNestedContexts();
            final int nestedContextCount = nested.size();

            final ContextDeque<Context<?>> done = new ContextDeque<>();
            final ContextDeque<Context<?>> failed = new ContextDeque<>();
            final CountDownLatch nestedLatch = new CountDownLatch(nestedContextCount);

            // run nested command's do for nested contexts
            getLog().debug("Running {} nested commands 'doCommand'", nestedContextCount);
            final var nestedStateListener = new NestedCommandStateChangedListener(done, failed, nestedLatch);

            executeNested(nested, nestedStateListener);

            // wait for all nested commands have done
            getLog().debug("Waiting for {} nested commands done", nestedContextCount);
            nestedLatch.await();

            // after execution of nested, success and fail dequeues processing
            postExecutionProcessing(context, done.deque, failed.deque, nested);
        } catch (InterruptedException e) {
            getLog().error("Could not wait nested do finished '{}' with input {}", getId(), inputParameter, e);
            context.failed(e);
            // Clean up whatever needs to be handled before interrupting
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            getLog().error("Cannot do Do of '{}' with input {}", getId(), inputParameter, e);
            context.failed(e);
        }
    }

    /**
     * To run execution for each macro-command's nested context
     * <BR/>sequential traversal of nested contexts
     *
     * @param contexts nested contexts to execute
     * @param listener listener of nested context-state-change
     * @see Context
     * @see Deque
     * @see Context.StateChangedListener
     * @see RootCommand#doAsNestedCommand(NestedCommandExecutionVisitor, Context, Context.StateChangedListener)
     */
    public void executeNested(final Deque<Context<?>> contexts, final Context.StateChangedListener listener) {
        contexts.forEach(context -> context.getCommand().doAsNestedCommand(this, context, listener));
    }

    /**
     * To rollback command's execution with correct context state (DONE)
     * <BR/> the type of command result doesn't matter
     *
     * @param context context of undo execution
     * @see Context
     * @see Context#getUndoParameter()
     * @see MacroCommand#rollbackNestedDone(Input)
     * @see MacroCommand#postRollbackProcessing(Context)
     */
    @Override
    public void executeUndo(final Context<?> context) {
        final Input<Deque<Context<?>>> parameter = context.getUndoParameter();
        getLog().debug("Do undo for {}", parameter);
        try {
            checkNullParameter(parameter);
            // rolling back successful nested do command contexts
            rollbackNestedDone(parameter);
            // after rollback process, check the contexts' states
            postRollbackProcessing(context);
        } catch (Exception e) {
            getLog().error("Cannot run undo for command:'{}'", getId(), e);
            context.failed(e);
        }
    }

    /**
     * To rollback changes for nested successful commands (contexts with state DONE)
     *
     * @param contexts collection of nested contexts with DONE state
     * @see NestedCommand#undoAsNestedCommand(NestedCommandExecutionVisitor, Context)
     * @see MacroCommand#undoNestedCommand(RootCommand, Context)
     * @see Context#isDone()
     */
    public Deque<Context<?>> rollbackNestedDone(final Input<Deque<Context<?>>> contexts) {
        return contexts.value().stream()
                .filter(Context::isDone)
                .map(this::undoNestedCommand)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * To rollback changes of nested command
     *
     * @param context nested context
     * @return context with nested command undo results
     */
    public Context<?> undoNestedCommand(final Context<?> context) {
        return context.getCommand().undoAsNestedCommand(this, context);
    }

    /**
     * To get final main do-command result from nested command-contexts
     *
     * @param nested nested command-contexts
     * @return the command result's value
     */
    @SuppressWarnings("unchecked")
    public T finalCommandResult(final Deque<Context<?>> nested) {
        return (T) nested.getLast().getResult().orElseThrow();
    }

    // private methods
    private void postExecutionProcessing(final Context<T> mainContext,
                                         final Deque<Context<?>> done,
                                         final Deque<Context<?>> failed,
                                         final Deque<Context<?>> nested) {
        if (mainContext instanceof CommandContext<T> mainCommandContext) {
            // check failed contexts
            if (ObjectUtils.isEmpty(failed)) {
                // no failed contexts
                final T result = finalCommandResult(nested);
                getLog().debug("Command executed well and returned {}", result);
                mainCommandContext.setUndoParameter(Input.of(done));
                mainCommandContext.setResult(result);
            } else {
                // found failed contexts, collect the exception
                mainCommandContext.failed(failed.getFirst().getException());
                // rollback all successful contexts
                final var doneContexts = Input.of(done);
                getLog().warn("Rolling back all successful commands {}", doneContexts);
                rollbackNestedDone(doneContexts);
            }
        } else {
            throw new UnableExecuteCommandException(getId());
        }
    }

    private <N> void postRollbackProcessing(Context<?> context) {
        final var undoneContexts = context.<Deque<Context<N>>>getUndoParameter().value();
        final var failedContext = undoneContexts.stream().filter(Context::isFailed).findFirst();

        if (failedContext.isEmpty()) {
            // no errors found
            getLog().debug("No errors found after undo is done");
            context.setState(Context.State.UNDONE);
        } else {
            final Exception undoneException = failedContext.orElseThrow().getException();
            getLog().warn("Wrong undo because", undoneException);
            // something went wrong during undo nested commands
            context.failed(undoneException);
            // rolling back nested undo changes calling nested.doAsNestedCommand(...)
            rollBackNestedUndone(undoneContexts);
        }
    }

    private <N> void rollBackNestedUndone(final Deque<Context<N>> undoneContexts) {
        getLog().debug("Rolling back from undone {} command(s)", undoneContexts.size());
        // to do rollback undone nested contexts
        undoneContexts.stream().filter(Context::isUndone).forEach(this::rollbackUndone);
    }

    private void rollbackUndone(Context<?> context) {
        final String logTemplate = "Changed state of '{}' from State:{} to :{}";
        final Context.StateChangedListener stateListener =
                (c, prev, curr) -> logStateChange(logTemplate, getId(), prev, curr);
        context.setState(Context.State.READY);
        context.getCommand().doAsNestedCommand(this, context, stateListener);
    }

    private void logStateChange(String logTemplate, String commandId, Context.State prev, Context.State curr) {
        getLog().debug(logTemplate, commandId, prev, curr);
    }

    // nested classes

    /**
     * Context state changed listener. According to state, distribute context to appropriate deque
     *
     * @see Context.State
     * @see Context.StateChangedListener#stateChanged(Context, Context.State, Context.State)
     * @see ContextDeque#putToTail(Object)
     * @see MacroCommand#executeNested(Deque, Context.StateChangedListener)
     */
    class NestedCommandStateChangedListener implements Context.StateChangedListener {
        final ContextDeque<Context<?>> doneContextsDeque;
        final ContextDeque<Context<?>> failedContextsDeque;
        final CountDownLatch nestedLatch;

        private NestedCommandStateChangedListener(final ContextDeque<Context<?>> doneContextsDeque,
                                                  final ContextDeque<Context<?>> failedContextsDeque,
                                                  final CountDownLatch nestedLatch) {
            this.doneContextsDeque = doneContextsDeque;
            this.failedContextsDeque = failedContextsDeque;
            this.nestedLatch = nestedLatch;
        }

        @Override
        public void stateChanged(Context<?> context, Context.State previous, Context.State current) {
            final String commandId = context.getCommand().getId();
            switch (current) {
                case INIT, READY, WORK, UNDONE -> {
                    final String logTemplate = "Changed state of '{}' from State:{} to :{}";
                    logStateChange(logTemplate, commandId, previous, current);
                }
                case CANCEL -> {
                    getLog().debug("Command '{}' is Canceled from State:{}", commandId, previous);
                    nestedLatch.countDown();
                }
                case DONE -> {
                    getLog().debug("Command '{}' is Done from State:{}", commandId, previous);
                    doneContextsDeque.putToTail(context);
                    nestedLatch.countDown();
                }
                case FAIL -> {
                    getLog().debug("Command '{}' is Failed from State:{}", commandId, previous);
                    failedContextsDeque.putToTail(context);
                    nestedLatch.countDown();
                }
                default -> throw new IllegalStateException("Unexpected value: " + current);
            }
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

        void putToTail(final T context) {
            locker.lock();
            try {
                deque.addLast(context);
            } finally {
                locker.unlock();
            }
        }
    }
}
