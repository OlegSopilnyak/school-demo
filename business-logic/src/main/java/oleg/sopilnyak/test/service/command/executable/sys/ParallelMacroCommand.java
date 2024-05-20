package oleg.sopilnyak.test.service.command.executable.sys;

import lombok.AllArgsConstructor;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.exception.CountDownLatchInterruptedException;
import org.springframework.scheduling.SchedulingTaskExecutor;

import java.util.Deque;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;

/**
 * Parallel MacroCommand: macro-command the command with nested commands inside.
 * Uses execution of nested commands simultaneously
 */
@AllArgsConstructor
public abstract class ParallelMacroCommand extends MacroCommand<SchoolCommand> {
    protected final SchedulingTaskExecutor commandContextExecutor;

    /**
     * To run macro-command's nested contexts<BR/>
     * Executing collection of nested command contexts
     *
     * @param doContexts    nested command contexts collection
     * @param stateListener listener of context-state-change
     * @see SchoolCommand#doAsNestedCommand(NestedCommandExecutionVisitor, Context, Context.StateChangedListener)
     * @see SchedulingTaskExecutor#submit(Callable)
     * @see Deque
     * @see Context
     * @see Context.StateChangedListener
     * @see Context.State#FAIL
     */
    @Override
    public  <T> void doNestedCommands(final Deque<Context<T>> doContexts,
                                        final Context.StateChangedListener<T> stateListener) {
        final CountDownLatch latch = new CountDownLatch(doContexts.size());
        // parallel walking through contexts set
        doContexts.forEach(context -> {
            getLog().debug("Submit executing of command: '{}' with context:{}", context.getCommand().getId(), context);
            final Callable<Context<T>> doRunner = () -> {
                try {
                    context.getCommand().doAsNestedCommand(this, context, stateListener);
                    return context;
                } catch (Exception e) {
                    context.failed(e);
                    getLog().error("Command do execution is failed", e);
                    return context;
                } finally {
                    latch.countDown();
                }
            };

            final Future<Context<T>> future = commandContextExecutor.submit(doRunner);

            // To test is doRunner starting well in commandContextExecutor
            if (future.isCancelled()) {
                getLog().warn("Canceled executing of command: '{}' with context:{}", context.getCommand().getId(), context);
                context.addStateListener(stateListener);
                context.setState(Context.State.CANCEL);
                context.removeStateListener(stateListener);
                latch.countDown();
            }
        });

        // waiting for CountDownLatch latch
        try {
            latch.await();
        } catch (InterruptedException e) {
            getLog().error("CountDownLatch is interrupted", e);
            // Clean up whatever needs to be handled before interrupting
            Thread.currentThread().interrupt();
            throw new CountDownLatchInterruptedException(latch.getCount(), e);
        }
    }

    /**
     * To rollback changes for contexts with state DONE<BR/>
     * sequential revers order of commands deque
     *
     * @param undoContexts collection of contexts with DONE state
     * @see SchedulingTaskExecutor#submit(Callable)
     * @see Deque
     * @see Context.State#DONE
     */
    @Override
    protected <T> Deque<Context<T>> rollbackDoneContexts(Deque<Context<T>> undoContexts) {
        final CountDownLatch latch = new CountDownLatch(undoContexts.size());

        // parallel walking through contexts set
        undoContexts.forEach(context -> {
            getLog().debug("Submit rolling back of command: '{}' with context:{}", context.getCommand().getId(), context);
            final Callable<Context<T>> undoRunner = () -> {
                try {
                    return rollbackDoneContext(context.getCommand(), context);
                } catch (Exception e) {
                    context.failed(e);
                    getLog().error("Rollback failed", e);
                    return context;
                } finally {
                    latch.countDown();
                }
            };

            final Future<Context<T>> future = commandContextExecutor.submit(undoRunner);

            // To test is undoRunner starting well in commandContextExecutor
            if (future.isCancelled()) {
                getLog().warn("Canceled rolling back of command: '{}' with context:{}", context.getCommand().getId(), context);
                context.setState(Context.State.CANCEL);
                latch.countDown();
            }
        });

        // waiting for CountDownLatch latch
        try {
            latch.await();
        } catch (InterruptedException e) {
            getLog().error("CountDownLatch is interrupted", e);
            // Clean up whatever needs to be handled before interrupting
            Thread.currentThread().interrupt();
            throw new CountDownLatchInterruptedException(latch.getCount(), e);
        }
        return undoContexts;
    }
}
