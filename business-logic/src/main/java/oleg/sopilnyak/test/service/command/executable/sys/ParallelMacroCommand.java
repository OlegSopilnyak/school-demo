package oleg.sopilnyak.test.service.command.executable.sys;


import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.type.base.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.ObjectUtils;

/**
 * MacroCommand: The command type with nested commands inside which are executing in parallel way.
 *
 * @param <T> the type of command execution (do) result
 * @see CompositeCommand
 * @see MacroCommand
 * @see SchedulingTaskExecutor
 * @see CompletableFuture
 */
public abstract class ParallelMacroCommand<T> extends MacroCommand<T> {
    protected final transient SchedulingTaskExecutor executor;
    protected ParallelMacroCommand(final ActionExecutor actionExecutor, final SchedulingTaskExecutor executor) {
        super(actionExecutor);
        this.executor = executor;
    }

    /**
     * To run do execution for each macro-command's nested command
     *
     * @param contexts nested command contexts to execute
     * @param listener listener of nested context-state-change
     * @return nested command contexts after execution
     * @see CompletableFuture#supplyAsync(Supplier, Executor)
     * @see CompletableFuture#get()
     * @see Context.State#READY
     * @see Deque
     * @see Context.StateChangedListener
     * @see CompositeCommand#executeDoNested(Context, Context.StateChangedListener)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Deque<Context<?>> executeNested(final Deque<Context<?>> contexts, final Context.StateChangedListener listener) {
        if (ObjectUtils.isEmpty(contexts)) {
            getLog().warn("Nothing to do");
            return contexts;
        }
        // launch nested contexts in separate threads
        final CompletableFuture<Context<?>>[] nestedCommandContextFutures = contexts.stream()
                .map(context -> launchNestedCommandDo(context, listener))
                .toArray(CompletableFuture[]::new);

        getLog().debug("Nested commands execution started for {} contexts", nestedCommandContextFutures.length);
        // wait for all commands execution finished
        CompletableFuture.allOf(nestedCommandContextFutures).join();

        getLog().debug("Nested commands execution finished for {} contexts", nestedCommandContextFutures.length);
        // collect result contexts and return
        return Arrays.stream(nestedCommandContextFutures)
                // getting context from completable future
                .map(this::getFrom).filter(Objects::nonNull)
                // collect not null context to resulting Deque
                .collect(Collectors.toCollection(LinkedList::new));
    }

    /**
     * To run rolling back execution for each macro-command's nested command
     *
     * @param contexts deque of contexts with DONE state
     * @see CompletableFuture#supplyAsync(Supplier, Executor)
     * @see CompletableFuture#get()
     * @see Context.State#DONE
     * @see Deque
     * @see Context
     * @see CompositeCommand#executeUndoNested(Context)
     */
    @SuppressWarnings("unchecked")
    @Override
    public Deque<Context<?>> rollbackNested(final Deque<Context<?>> contexts) {
        if (ObjectUtils.isEmpty(contexts)) {
            getLog().warn("Nothing to rollback");
            return contexts;
        }
        // launch nested command undo in separate threads
        final CompletableFuture<Context<?>>[] nestedCommandContextFutures = contexts.stream()
                .map(this::launchNestedCommandUndo)
                .toArray(CompletableFuture[]::new);

        getLog().debug("Nested commands rollback started for {} contexts", nestedCommandContextFutures.length);
        // waiting for all nested command execution done
        CompletableFuture.allOf(nestedCommandContextFutures).join();

        getLog().debug("Nested commands rollback finished for {} contexts", nestedCommandContextFutures.length);
        // collect result contexts and return
        return Arrays.stream(nestedCommandContextFutures)
                // getting context from completable future
                .map(this::getFrom).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedList::new));
    }

    // private methods
    // launch nested command DO
    private CompletableFuture<Context<?>> launchNestedCommandDo(final Context<?> context, final Context.StateChangedListener listener) {
        return launchNestedCommandWith(() -> executeDoNested(context, listener));
    }

    // launch nested command UNDO
    private CompletableFuture<Context<?>> launchNestedCommandUndo(final Context<?> context) {
        return launchNestedCommandWith(() -> executeUndoNested(context));
    }

    // run nested command execution in the separate thread
    private CompletableFuture<Context<?>> launchNestedCommandWith(final Supplier<Context<?>> commandExecution) {
        // prepare processing context for execute command execution of the nested command
        final ActionContext actionContext = ActionContext.current();
        return CompletableFuture.supplyAsync(() -> {
            try {
                // setup processing context for the thread of threads pool
                ActionContext.install(actionContext);
                return commandExecution.get();
            } finally {
                // release current thread
                ActionContext.release();
            }
        }, executor);
    }

    // restore execution context from completable future
    private Context<?> getFrom(final CompletableFuture<Context<?>> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            getLog().error("Cannot get context from completable future.", e);
            /* Clean up whatever needs to be handled before interrupting  */
            Thread.currentThread().interrupt();
            return null;
        }
    }

}
