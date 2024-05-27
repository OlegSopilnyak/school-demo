package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.NestedCommand;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Command-Base: macro-command the command with nested commands inside
 */
public abstract class MacroCommand<T extends NestedCommand>
        implements CompositeCommand<T>,
        NestedCommandExecutionVisitor, PrepareContextVisitor {
    private final List<T> commands = new LinkedList<>();

    /**
     * To get the collection of commands used it composite
     *
     * @return collection of included commands
     */
    @Override
    public Collection<T> commands() {
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
    public void add(final T command) {
        synchronized (commands) {
            commands.add(command);
        }
    }

    /**
     * To create command's context with doParameter
     *
     * @param input context's doParameter value
     * @param <T>   type of command result
     * @return context instance
     * @see Context
     * @see Context#getRedoParameter()
     * @see CommandContext
     * @see Context.State#READY
     * @see SchoolCommand#createContext()
     */
    @Override
    public  <R> Context<R> createContext(Object input) {
        final MacroCommandParameter<R> doParameter = new MacroCommandParameter<>(input,
                this.commands().stream().map(SchoolCommand.class::cast)
                        .<Context<R>>map(command -> command.doNested(this, command, input)
//                                acceptPreparedContext(this, input)
                        )
                        .collect(Collectors.toCollection(LinkedList::new))
        );
        // assemble input parameter contexts for redo
        return CompositeCommand.super.createContext(doParameter);
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
    public <R> void executeDo(final Context<R> doContext) {
        final Object input = doContext.getRedoParameter();
        getLog().debug("Do redo for {}", input);
        try {
            final MacroCommandParameter<R> wrapper = commandParameter(input);

            final ContextDeque<Context<R>> doneContextsDeque = new ContextDeque<>();
            final ContextDeque<Context<R>> failedContextsDeque = new ContextDeque<>();

            final Deque<Context<R>> nestedContexts = wrapper.getNestedContexts();

            final CountDownLatch executed = new CountDownLatch(nestedContexts.size());
            final Context.StateChangedListener<R> stateListener = (context, previous, newOne) -> {
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
     * @see NestedCommand#doAsNestedCommand(NestedCommandExecutionVisitor, Context, Context.StateChangedListener)
     * @see Deque
     * @see Context.StateChangedListener
     */
    public <R> void doNestedCommands(final Deque<Context<R>> doContexts,
                                     final Context.StateChangedListener<R> stateListener) {
        doContexts.forEach(context -> {
            final var nested = context.getCommand();
            nested.doAsNestedCommand(this, context, stateListener);
        });
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
            // rolling back successful nested commands
            rollbackDoneContexts(doneContexts);
            // after rollback process check the contexts' states
            afterRollbackDoneCheck(doneContexts);
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
     * @param doneContexts collection of nested contexts with DONE state
     * @see SchoolCommand#undoAsNestedCommand(NestedCommandExecutionVisitor, Context)
     * @see MacroCommand#undoNestedCommand(SchoolCommand, Context)
     * @see Context.State#DONE
     */
    protected <T> Deque<Context<T>> rollbackDoneContexts(final Deque<Context<T>> doneContexts) {
        return doneContexts.stream()
                .map(context -> {
                    final var nested = context.getCommand();
                    nested.undoAsNestedCommand(this, context);
                    return context;
                })
                .collect(Collectors.toCollection(LinkedList::new));
    }

// For commands playing Nested Command Role

    /**
     * To get access to command instance as nested one
     *
     * @return the reference to the command instance
     * @see NestedCommand#asNestedCommand()
     */
    @Override
    public MacroCommand<T> asNestedCommand() {
        return this;
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
