package oleg.sopilnyak.test.service.command.executable.sys;


import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.type.base.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
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
    private final SchedulingTaskExecutor executor;
    protected ParallelMacroCommand(ActionExecutor actionExecutor, SchedulingTaskExecutor executor) {
        super(actionExecutor);
        this.executor = executor;
    }

    /**
     * To run do execution for each macro-command's nested contexts in READY state
     *
     * @param contexts nested contexts to execute
     * @param listener listener of nested context-state-change
     * @return nested contexts after execution
     * @see Context.State#READY
     * @see Deque
     * @see Context.StateChangedListener
     * @see CompositeCommand#executeDoNested(Context, Context.StateChangedListener)
     */
    @Override
    public Deque<Context<?>> executeNested(Deque<Context<?>> contexts, Context.StateChangedListener listener) {
        if (ObjectUtils.isEmpty(contexts)) {
            getLog().warn("Nothing to do");
            return contexts;
        }
        return null;
    }

}
