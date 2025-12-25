package oleg.sopilnyak.test.service.command.executable.sys;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.base.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedContextDeque;
import oleg.sopilnyak.test.service.command.type.nested.NestedStateChangedListener;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;

import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.util.ObjectUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Command-Base: macro-command the command with nest of commands inside
 *
 * @param <T> the type of command execution (do) result
 * @see NestedCommand
 * @see RootCommand
 * @see CompositeCommand
 */
@RequiredArgsConstructor
public abstract class MacroCommand<T> implements CompositeCommand<T> {
    @Getter
    // commands executor for the commands from the nest
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
     * @param <N>     the result type of the nested command
     * @see NestedCommand
     */
    @Override
    public <N> boolean putToNest(final NestedCommand<N> command) {
        synchronized (netsedCommandsList) {
            return netsedCommandsList.add(command);
        }
    }

    /**
     * To execute command with correct context state
     *
     * @param context context of execution
     * @see Context
     * @see Context#getRedoParameter()
     * @see MacroCommandParameter#getNestedContexts()
     * @see NestedContextDeque
     * @see NestedStateChangedListener
     * @see MacroCommand#executeNested(Deque, Context.StateChangedListener)
     * @see CountDownLatch#await()
     * @see MacroCommand#afterExecutionProcessing(Context, Deque, Deque, Deque)
     */
    @Override
    public void executeDo(final Context<T> context) {
        // preparing command's input for processing
        final Input<MacroCommandParameter> inputParameter = context.getRedoParameter();
        try {
            checkNullParameter(inputParameter);
            // preparing multi-contexts parameter
            final MacroCommandParameter mainCommandParameter = inputParameter.value();
            // checking input parameter integrity
            checkNullParameter(mainCommandParameter);
            checkNullNested(mainCommandParameter);
            getLog().debug("Doing Execution For {}", mainCommandParameter);
            final Deque<Context<?>> nestedContexts = mainCommandParameter.getNestedContexts();
            // nested command-contexts counter
            final int nestedContextCount = nestedContexts.size();
            final CountDownLatch nestedLatch = new CountDownLatch(nestedContextCount);
            //
            // run nested command's do for nested contexts
            getLog().debug("Running {} nested commands execution (doCommand)", nestedContextCount);
            // preparing queues for results separation by command-context-state
            final NestedContextDeque<Context<?>> succeed = new NestedContextDeque<>();
            final NestedContextDeque<Context<?>> failed = new NestedContextDeque<>();
            // the listener for command-context-state queues above
            final var nestedStateListener = new NestedStateChangedListener(succeed, failed, nestedLatch, getLog());
            //
            // executing nested commands using their contexts and collect the results
            final Deque<Context<?>> executionNestedResults = executeNested(nestedContexts, nestedStateListener);
            //
            // wait for all nested command executions have done
            getLog().debug("Waiting for {} nested commands done", nestedContextCount);
            nestedLatch.await();
            //
            // updating command-context input (redo) parameter's contexts
            updateMacroCommandParameter(context, executionNestedResults);
            //
            // after execution of nested, success and fail dequeues processing
            afterExecutionProcessing(context, succeed.getDeque(), failed.getDeque(), executionNestedResults);
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
     * @see MacroCommand#rollbackNested(Deque)
     * @see MacroCommand#afterRollbackProcessing(Context, Deque)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void executeUndo(final Context<?> context) {
        final Input<Deque<Context<?>>> parameter = context.getUndoParameter();
        getLog().debug("Do undo for {}", parameter);
        try {
            // shouldn't be null
            checkNullParameter(parameter);
            // rolling back successful nested do command contexts
            final Deque<Context<?>> rollbackResult = rollbackNested(parameter.value());
            // after rollback process, check the resulting contexts and update main one
            afterRollbackProcessing((Context<T>) context, rollbackResult);
        } catch (Exception e) {
            getLog().error("Cannot run undo for command:'{}'", getId(), e);
            context.failed(e);
        }
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
        return contexts.stream().filter(Context::isDone).map(this::executeUndoNested)
                .collect(Collectors.toCollection(LinkedList::new));
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

    /**
     * Post-processing nested commands execution contexts
     *
     * @param rootContext    main context of macro-command
     * @param successful collection of successful nested contexts
     * @param failed     collection of failed nested contexts
     * @param nestedResultsAfter      collection of all nested contexts after nested commands execution
     * @see Deque
     * @see Context
     * @see CommandContext
     * @see MacroCommand#afterProcessSuccessfulExecution(CommandContext, Deque, Deque)
     * @see MacroCommand#rollbackSuccessfulNestedCommands(CommandContext, Deque, Deque)
     */
    protected void afterExecutionProcessing(
            final Context<T> rootContext,
            final Deque<Context<?>> successful, final Deque<Context<?>> failed, final Deque<Context<?>> nestedResultsAfter
    ) {
        if (rootContext instanceof CommandContext<T> rootCommandContext) {
            // check failed contexts
            if (ObjectUtils.isEmpty(failed)) {
                // no failed contexts
                afterProcessSuccessfulExecution(rootCommandContext, successful, nestedResultsAfter);
            } else {
                // found failed contexts, collect the exception
                rollbackSuccessfulNestedCommands(rootCommandContext, successful, failed);
            }
        } else {
            throw new UnableExecuteCommandException(getId());
        }
    }

    /**
     * Post-processing nested commands rolling back contexts
     *
     * @param rootContext        main context of macro-command
     * @param rollbackResult collection of all nested contexts after nested commands rollback
     */
    protected void afterRollbackProcessing(final Context<T> rootContext, final Deque<Context<?>> rollbackResult) {
        final var failedContext = rollbackResult.stream().filter(Context::isFailed).findFirst();
        // check the failed nested context existence
        if (failedContext.isEmpty()) {
            // no errors found
            getLog().debug("No errors found after undo is done");
            // updating command-context input (redo) parameter's contexts
            updateMacroCommandParameter(rootContext, rollbackResult);
            rootContext.setState(Context.State.UNDONE);
            return;
        }
        // there is one at least failed nested context there
        final Exception undoneException = failedContext.get().getException();
        getLog().warn("Wrong undo because", undoneException);
        // something went wrong during undo nested commands
        rootContext.failed(undoneException);
        // rolling back nested rollback changes, sounds crazy I agree
        // doing execution for all successfully undone nested contexts
        final Deque<Context<?>> undoNestedContexts = rollbackResult.stream()
                .map(context -> context.isUndone() ? rollbackNestedUndo(context) : context)
                .collect(Collectors.toCollection(LinkedList::new));
        // updating command-context input (redo) parameter's contexts
        updateMacroCommandParameter(rootContext, undoNestedContexts);
    }

    // private methods
    private Context<?> rollbackNestedUndo(final Context<?> nestedContext) {
        nestedContext.setState(Context.State.READY);
        return executeDoNested(nestedContext);
    }

    private void checkNullNested(final MacroCommandParameter input) {
        input.getNestedContexts().forEach(context -> {
            if (isNull(context)) {
                getLog().error("Nested context is null in parameter {}", input);
                throw new NullPointerException("Nested context is null");
            }
        });
    }

    private void afterProcessSuccessfulExecution(
            final CommandContext<T> rootContext, final Deque<Context<?>> successful, final Deque<Context<?>> nestedResultsAfter
    ) {
        // getting the result of command's execution
        final T result = finalCommandResult(nestedResultsAfter);
        getLog().debug("Command executed well and returned {}", result);
        // setup undo parameter of the context
        rootContext.setUndoParameter(Input.of(successful));
        // store command successful execution result
        rootContext.setResult(result);
    }

    private void rollbackSuccessfulNestedCommands(
            final CommandContext<T> rootContext, final Deque<Context<?>> successful, final Deque<Context<?>> failed
    ) {
        rootContext.failed(failed.getFirst().getException());
        getLog().warn("Rolling back all successful nested commands {}", successful);
        // rollback all successful contexts
        rollbackCompositeCommand(rootContext, successful);
    }

    private void rollbackCompositeCommand(final Context<T> rootContext, final Deque<Context<?>> nestedCommandsToRollback) {
        // rollback all successful contexts
        final Deque<Context<?>> rollbackResult = rollbackNested(nestedCommandsToRollback);
        getLog().debug("Rolled back {} nested commands", rollbackResult.size());
        // updating command-context input (redo) parameter's contexts
        updateMacroCommandParameter(rootContext, rollbackResult);
    }

    private void updateMacroCommandParameter(final Context<T> rootContext, final Deque<Context<?>> result) {
        final MacroCommandParameter inputParameter = rootContext.<MacroCommandParameter>getRedoParameter().value();
        final Map<String, Context<?>> rolledbackMap = result.stream()
                .collect(Collectors.toMap(MacroCommand::contextCommandId, Function.identity()));
        final Deque<Context<?>> updatedNestedContexts = inputParameter.getNestedContexts().stream()
                .map(context -> updated(context, rolledbackMap))
                .collect(Collectors.toCollection(LinkedList::new));
        inputParameter.updateNestedContexts(updatedNestedContexts);
    }

    private static Context<?> updated(final Context<?> context, final Map<String, Context<?>> updated) {
        return updated.getOrDefault(contextCommandId(context), context);
    }

    private static String contextCommandId(final Context<?> context) {
        return context.getCommand().getId();
    }
}
