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
 * Command-Base: command of couple commands
 */
public abstract class MacroCommand<T> implements CompositeCommand<T> {
    private final List<SchoolCommand> commands = Collections.synchronizedList(new LinkedList<>());

    /**
     * To get the collection of commands used it composite
     *
     * @return collection of included commands
     */
    @Override
    public Collection<SchoolCommand> commands() {
        return Collections.unmodifiableList(commands);
    }

    /**
     * To add the command
     *
     * @param command the instance to add
     * @see SchoolCommand
     */
    @Override
    public void add(SchoolCommand command) {
        commands.add(command);
    }

    /**
     * To execute command's business-logic
     *
     * @param parameter command's parameter
     * @return execution's result
     */
    @Override
    public CommandResult<T> execute(Object parameter) {
        return CommandResult.<T>builder().success(false).result(Optional.empty()).build();
    }

    /**
     * To create command's context with doParameter
     *
     * @param input context's doParameter value
     * @return context instance
     * @see Context
     * @see Context#getDoParameter()
     * @see CommandContext
     * @see Context.State#READY
     */
    @Override
    public Context<T> createContext(Object input) {
        final Context<T> context = createContext();
        final Deque<Context> contexts = commands().stream()
                .map(cmd -> prepareContext(cmd, input))
                .collect(Collectors.toCollection(LinkedList::new));

        context.setDoParameter(CommandParameterWrapper.<T>builder().input(input).nestedContexts(contexts).build());
        return context;
    }

    /**
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context.State#WORK
     */
    @Override
    public void doRedo(Context<T> context) {
        final Object input = context.getDoParameter();
        getLog().debug("Do redo for {}", input);
        try {
            final CommandParameterWrapper wrapper = commandParameter(input);

            final Deque<Context> done = new LinkedList<>();
            final Lock doneLock = new ReentrantLock();

            final Deque<Context> fail = new LinkedList<>();
            final Lock failLock = new ReentrantLock();

            final Deque<Context> nested = wrapper.getNestedContexts();
            final CountDownLatch executed = new CountDownLatch(nested.size());
            final Context.StateChangedListener listener = (ctx, previous, newOne) -> {
                switch (newOne) {
                    case INIT, READY, WORK, UNDONE -> {
                    }
                    case DONE -> {
                        addContextInto(done, ctx, doneLock);
                        executed.countDown();
                    }
                    case FAIL -> {
                        addContextInto(fail, ctx, failLock);
                        executed.countDown();
                    }
                    default -> throw new IllegalStateException("Unexpected value: " + newOne);
                }
            };

            // run nested contexts
            runNestedContexts(nested, listener);
            // wait for all commands finished
            executed.await();

            // check failed contexts
            if (fail.isEmpty()) {
                // no failed contexts
                context.setUndoParameter(done);
                context.setResult((T) nested.getLast().getResult().orElse(null));
            } else {
                // exists failed contexts
                context.failed(fail.getFirst().getException());
                // rollback all done contexts
                rollback(done);
            }
        } catch (Exception e) {
            getLog().error("Cannot do redo of '{}' with input {}", getId(), input, e);
            context.failed(e);
        }
    }

    /**
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    @Override
    public void doUndo(Context<T> context) {
        final Object parameter = context.getUndoParameter();
        getLog().debug("Do undo for {}", parameter);
        try {
            final Deque<Context> done = commandParameter(parameter);
            rollback(done);
            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            getLog().error("Cannot run redo for command:'{}'", getId(), e);
            context.failed(e);
        }
    }

    /**
     * To prepare context for particular command
     *
     * @param command   command instance
     * @param parameter macro-command input parameter
     * @return built context of the command for input parameter
     * @see SchoolCommand
     * @see SchoolCommand#createContext(Object)
     * @see Context
     */
    protected Context prepareContext(SchoolCommand command, Object parameter) {
        return command.createContext(parameter);
    }

    /**
     * To run macro-command's nested contexts
     *
     * @param nestedContexts   nested contexts collection
     * @param listener listener of context-state-change
     * @see MacroCommand#createContext(Object)
     * @see Deque
     * @see LinkedList
     * @see Context
     * @see Context.StateChangedListener
     */
    protected void runNestedContexts(final Deque<Context> nestedContexts, final Context.StateChangedListener listener) {
        nestedContexts.forEach(ctx -> runNestedCommand(ctx, listener));
    }

    /**
     * To execute one nested command
     *
     * @param context  nested command execution context
     * @param listener the lister of command state change
     */
    protected Context runNestedCommand(final Context context, final Context.StateChangedListener listener) {
        context.addStateListener(listener);
        final SchoolCommand command = context.getCommand();
        try {
            command.redo(context);
        } catch (Exception e) {
            getLog().error("Cannot run redo for command:'{}'", command.getId(), e);
            context.failed(e);
        } finally {
            context.removeStateListener(listener);
        }
        return context;
    }

    // private methods

    private static void addContextInto(final Deque<Context> contexts, final Context context, final Lock locker) {
        locker.lock();
        try {
            contexts.add(context);
        } finally {
            locker.unlock();
        }
    }

    private static void rollback(final Deque<Context> done) {
        done.forEach(ctx -> ctx.getCommand().undo(ctx));
    }
}
