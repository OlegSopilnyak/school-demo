package oleg.sopilnyak.test.service.command.executable.core;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.ObjectUtils;
import lombok.SneakyThrows;

/**
 * Parallel MacroCommand: macro-command the command with nested commands inside.
 *
 * @param <T> the type of command execution (do) result
 * @see MacroCommand
 * @see SchedulingTaskExecutor
 * @see CompletableFuture
 * @deprecated
 */
@Deprecated
public abstract class LegacyParallelMacroCommand<T> extends MacroCommand<T> {
    protected LegacyParallelMacroCommand(CommandActionExecutor actionExecutor) {
        super(actionExecutor);
    }

    /**
     * To get access to command's nested command-context executor
     *
     * @return instance of executor
     */
    public abstract SchedulingTaskExecutor getExecutor();

    /**
     * To run execution of macro-command's nested contexts<BR/>
     * Executing deque of nested command contexts
     *
     * @param readyContexts deque of contexts with READY state
     * @param stateListener listener of context-state-change
     * @see CompletableFuture
     * @see LegacyParallelMacroCommand#kickOffDoRunner(BlockingQueue, Context, Context.StateChangedListener)
     * @see BlockingQueue
     * @see Deque
     * @see Context
     * @see Context.StateChangedListener
     * @see Context.State#READY
     */
    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public Deque<Context<?>> executeNested(final Deque<Context<?>> readyContexts, final Context.StateChangedListener stateListener) {
        if (ObjectUtils.isEmpty(readyContexts)) {
            getLog().warn("Nothing to do");
            return readyContexts;
        }
        final int actionsQuantity = readyContexts.size();
        // Queue for nested command actions
        final BlockingQueue<ParallelCommandsInRootTransaction.ActionInRootTransaction<Context<?>>> actionsQueue =
                new LinkedBlockingQueue<>(actionsQuantity);
        // parallel walking through contexts set
        final CompletableFuture<Context<?>>[] runningContexts = readyContexts.stream()
                .map(context -> kickOffDoRunner(actionsQueue, context, stateListener))
                .toArray(CompletableFuture[]::new);
        // process actions from other running threads
        processingActivitiesQueue(actionsQueue, actionsQuantity);
        // waiting for all nested command execution done
        CompletableFuture.allOf(runningContexts).join();
        // collect result contexts and return
        return Arrays.stream(runningContexts).map(this::getFrom).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedList::new));
    }


    /**
     * To run rolling back of macro-command's nested contexts<BR/>
     * Executing deque of nested command contexts
     *
     * @param doneContexts deque of contexts with DONE state
     * @see CompletableFuture
     * @see LegacyParallelMacroCommand#kickOffUndoRunner(BlockingQueue, Context)
     * @see BlockingQueue
     * @see Deque
     * @see Context
     * @see Context.State#DONE
     */
    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public Deque<Context<?>> rollbackNested(final Deque<Context<?>> doneContexts) {
        if (ObjectUtils.isEmpty(doneContexts)) {
            getLog().warn("Nothing to undo");
            return doneContexts;
        }
        final int actionsQuantity = doneContexts.size();
        // Queue for nested command actions
        final BlockingQueue<ParallelCommandsInRootTransaction.ActionInRootTransaction<Context<?>>> actionsQueue =
                new LinkedBlockingQueue<>(actionsQuantity);
        // parallel walking through contexts set
        final CompletableFuture<Context<?>>[] runningContexts = doneContexts.stream()
                .map(context -> kickOffUndoRunner(actionsQueue, context))
                .toArray(CompletableFuture[]::new);
        // process actions from other threads
        processingActivitiesQueue(actionsQueue, actionsQuantity);
        // waiting for all nested command execution done
        CompletableFuture.allOf(runningContexts).join();
        // collect result contexts and return
        return Arrays.stream(runningContexts).map(this::getFrom).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    // private methods
    private Context<?> getFrom(CompletableFuture<Context<?>> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            getLog().error("Cannot get context from future.", e);
            return null;
        }
    }

    private void processingActivitiesQueue(
            final BlockingQueue<ParallelCommandsInRootTransaction.ActionInRootTransaction<Context<?>>> actionsQueue,
            final int capacity) {
        getLog().debug("Doing in current transaction {} parallel commands.", capacity);
        IntStream.range(0, capacity).forEach(i -> {
            getLog().debug("Processing in current transaction #{} parallel command.", (i + 1));
            try {
                actionsQueue.take().actionInRootTransaction();
            } catch (InterruptedException e) {
                getLog().error("Interrupted", e);
                /* Clean up whatever needs to be handled before interrupting  */
                Thread.currentThread().interrupt();
                throw new UnableExecuteCommandException("unknown", e);
            }
        });
    }

    private CompletableFuture<Context<?>> kickOffDoRunner(
            final BlockingQueue<ParallelCommandsInRootTransaction.ActionInRootTransaction<Context<?>>> actionsQueue,
            final Context<?> context, final Context.StateChangedListener stateListener) {
        getLog().debug("Submit executing of command: '{}' with context:{}", context.getCommand().getId(), context);
        final var runner = new DoNestedCommandRunner(actionsQueue, context, stateListener, ActionContext.current());
        return CompletableFuture.supplyAsync(runner::action, getExecutor());
    }

    private CompletableFuture<Context<?>> kickOffUndoRunner(
            final BlockingQueue<ParallelCommandsInRootTransaction.ActionInRootTransaction<Context<?>>> actionsQueue,
            final Context<?> context) {
        getLog().debug("Submit rolling back of command: '{}' with context:{}", context.getCommand().getId(), context);
        final var runner = new UndoNestedCommandRunner(actionsQueue, context, ActionContext.current());
        return CompletableFuture.supplyAsync(runner::action, getExecutor());
    }

    // private classes methods
    private class DoNestedCommandRunner extends ParallelCommandsInRootTransaction<Context<?>> {
        final Context<?> context;
        final Context.StateChangedListener stateListener;
        final ActionContext actionContext;

        public DoNestedCommandRunner(final BlockingQueue<ActionInRootTransaction<Context<?>>> actionsQueue,
                                     final Context<?> context, final Context.StateChangedListener stateListener,
                                     final ActionContext actionContext) {
            super(actionsQueue);
            this.context = context;
            this.stateListener = stateListener;
            this.actionContext = actionContext;
        }

        public Context<?> action() {
            try {
                ActionContext.install(actionContext);
                return runActionAndWait(() -> LegacyParallelMacroCommand.this.executeDoNested(context, stateListener));
            } catch (Throwable e) {
                getLog().error("Cannot execute doNested command", e);
                return null;
            } finally {
                ActionContext.release();
            }
        }

        @Override
        Logger getLog() {
            return LegacyParallelMacroCommand.this.getLog();
        }
    }

    private class UndoNestedCommandRunner extends ParallelCommandsInRootTransaction<Context<?>> {
        final Context<?> context;
        final ActionContext actionContext;

        public UndoNestedCommandRunner(final BlockingQueue<ActionInRootTransaction<Context<?>>> actionsQueue,
                                       final Context<?> context, final ActionContext actionContext) {
            super(actionsQueue);
            this.context = context;
            this.actionContext = actionContext;
        }

        public Context<?> action() {
            try {
                ActionContext.install(actionContext);
                return runActionAndWait(() -> LegacyParallelMacroCommand.this.executeUndoNested(context));
            } catch (Throwable e) {
                getLog().error("Cannot execute undoNested command", e);
                return null;
            } finally {
                ActionContext.release();
            }
        }

        @Override
        Logger getLog() {
            return LegacyParallelMacroCommand.this.getLog();
        }
    }
}
