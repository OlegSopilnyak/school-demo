package oleg.sopilnyak.test.service.command.executable.sys;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.sys.context.NestedContextDeque;
import oleg.sopilnyak.test.service.command.executable.sys.context.NestedStateChangedListener;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.base.CompositeCommand;
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
@AllArgsConstructor
public abstract class MacroCommand<T> implements CompositeCommand<T>, NestedCommandExecutionVisitor {
    // Nested commands executor
    @Getter
    private final transient ActionExecutor actionExecutor;
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
     * @see oleg.sopilnyak.test.service.command.executable.sys.context.NestedStateChangedListener
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
            final Deque<Context<?>> nestedContexts = mainCommandParameter.getNestedContexts();
            final int nestedContextCount = nestedContexts.size();

            final NestedContextDeque<Context<?>> succeed = new NestedContextDeque<>();
            final NestedContextDeque<Context<?>> failed = new NestedContextDeque<>();
            final CountDownLatch nestedLatch = new CountDownLatch(nestedContextCount);

            // run nested command's do for nested contexts
            getLog().debug("Running {} nested commands 'doCommand'", nestedContextCount);
            final var nestedStateListener = new NestedStateChangedListener(succeed, failed, nestedLatch, getLog());

            Deque<Context<?>> after = executeNested(nestedContexts, nestedStateListener);

            // wait for all nested commands have done
            getLog().debug("Waiting for {} nested commands done", nestedContextCount);
            nestedLatch.await();

            // after execution of nested, success and fail dequeues processing
            postExecutionProcessing(context, succeed.getDeque(), failed.getDeque(), nestedContexts);
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
            // shouldn't be null
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
        return rollbackNested(contexts.value());
    }

    /**
     * To run undo execution for each macro-command's nested contexts in DONE state
     *
     * @param contexts nested contexts to execute
     * @return nested contexts after execution
     * @see Context.State#DONE
     * @see Deque
     * @see CompositeCommand#executeUndoNested(Context)
     */
    @Override
    public Deque<Context<?>> rollbackNested(Deque<Context<?>> contexts) {
        return contexts.stream().filter(Context::isDone).map(this::undoNestedCommand)
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

    protected void postExecutionProcessing(final Context<T> mainContext,
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

    protected <N> void postRollbackProcessing(Context<?> context) {
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

    // private methods
    private <N> void rollBackNestedUndone(final Deque<Context<N>> undoneContexts) {
        getLog().debug("Rolling back from undone {} command(s)", undoneContexts.size());
        // to do rollback undone nested contexts
        undoneContexts.stream().filter(Context::isUndone).forEach(this::rollbackUndone);
    }

    private void rollbackUndone(Context<?> context) {
        final Context.StateChangedListener stateListener = (c, prev, curr) -> logStateChange(getId(), prev, curr);
        context.setState(Context.State.READY);
        context.getCommand().doAsNestedCommand(this, context, stateListener);
    }

    private void logStateChange(String commandId, Context.State prev, Context.State curr) {
        final String logTemplate = "Changed state of '{}' from State:{} to :{}";
        getLog().debug(logTemplate, commandId, prev, curr);
    }
}
