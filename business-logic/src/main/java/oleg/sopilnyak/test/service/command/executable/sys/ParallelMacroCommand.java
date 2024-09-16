package oleg.sopilnyak.test.service.command.executable.sys;

import lombok.AllArgsConstructor;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
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
public abstract class ParallelMacroCommand extends MacroCommand {
    /**
     * To get access to command's command-context executor
     *
     * @return instance of executor
     */
    public abstract SchedulingTaskExecutor getExecutor();

    /**
     * To run do of macro-command's nested contexts<BR/>
     * Executing collection of nested command contexts
     *
     * @param doContexts    nested command contexts collection
     * @param stateListener listener of context-state-change
     * @see RootCommand#doAsNestedCommand(NestedCommandExecutionVisitor, Context, Context.StateChangedListener)
     * @see SchedulingTaskExecutor#submit(Callable)
     * @see Deque
     * @see Context
     * @see Context.StateChangedListener
     * @see Context.State#FAIL
     */
    @Override
    public <T> void doNestedCommands(final Deque<Context<T>> doContexts,
                                     final Context.StateChangedListener<T> stateListener) {
        final CountDownLatch latch = new CountDownLatch(doContexts.size());
        // parallel walking through contexts set
        doContexts.forEach(context -> kickOffDoRunner(context, stateListener, latch));
        // waiting for CountDownLatch latch
        try {
            latch.await();
        } catch (InterruptedException e) {
            processInterruptedException(e, latch);
        }
    }


    /**
     * To rollback changes for contexts with state DONE<BR/>
     * sequential revers order of commands deque
     *
     * @param doneContexts collection of contexts with DONE state
     * @see SchedulingTaskExecutor#submit(Callable)
     * @see Deque
     * @see Context.State#DONE
     */
    @Override
    public <T> Deque<Context<T>> undoNestedCommands(Deque<Context<T>> doneContexts) {
        final CountDownLatch latch = new CountDownLatch(doneContexts.size());

        // parallel walking through contexts set
        doneContexts.forEach(context -> kickOffUndoRunner(context, latch));

        // waiting for CountDownLatch latch
        try {
            latch.await();
        } catch (InterruptedException e) {
            processInterruptedException(e, latch);
        }
        return doneContexts;
    }

    // private methods
    private void processInterruptedException(InterruptedException e, CountDownLatch latch) {
        getLog().error("CountDownLatch is interrupted", e);
        // Clean up whatever needs to be handled before interrupting
        Thread.currentThread().interrupt();
        throw new CountDownLatchInterruptedException(latch.getCount(), e);
    }

    private <T> void kickOffDoRunner(final Context<T> context,
                                     final Context.StateChangedListener<T> stateListener,
                                     final CountDownLatch latch) {
        getLog().debug("Submit executing of command: '{}' with context:{}", context.getCommand().getId(), context);
        final Future<Context<T>> future = getExecutor().submit(new DoCommandRunner<>(context, stateListener, latch));

        // To test is doRunner starting well in commandContextExecutor
        if (future.isCancelled()) {
            getLog().warn("Canceled executing of command: '{}' with context:{}", context.getCommand().getId(), context);
            context.addStateListener(stateListener);
            context.setState(Context.State.CANCEL);
            context.removeStateListener(stateListener);
            latch.countDown();
        }
    }

    private <T> void kickOffUndoRunner(final Context<T> context,
                                       final CountDownLatch latch) {
        getLog().debug("Submit rolling back of command: '{}' with context:{}", context.getCommand().getId(), context);
        final Future<Context<T>> future = getExecutor().submit(new UndoCommandRunner<>(context, latch));

        // To test is undoRunner starting well in commandContextExecutor
        if (future.isCancelled()) {
            getLog().warn("Canceled rolling back of command: '{}' with context:{}", context.getCommand().getId(), context);
            context.setState(Context.State.CANCEL);
            latch.countDown();
        }
    }

    // private classes methods
    private class DoCommandRunner<T> implements Callable<Context<T>> {
        final Context<T> context;
        final Context.StateChangedListener<T> stateListener;
        final CountDownLatch latch;

        public DoCommandRunner(Context<T> context, Context.StateChangedListener<T> stateListener, CountDownLatch latch) {
            this.context = context;
            this.stateListener = stateListener;
            this.latch = latch;
        }

        @Override
        public Context<T> call() {
            try {
                context.getCommand().doAsNestedCommand(ParallelMacroCommand.this, context, stateListener);
                return context;
            } catch (Exception e) {
                context.failed(e);
                getLog().error("Command do execution is failed", e);
                return context;
            } finally {
                latch.countDown();
            }
        }
    }

    private class UndoCommandRunner<T> implements Callable<Context<T>> {
        final Context<T> context;
        final CountDownLatch latch;

        public UndoCommandRunner(Context<T> context, CountDownLatch latch) {
            this.context = context;
            this.latch = latch;
        }

        @Override
        public Context<T> call() {
            try {
                final NestedCommand nested = context.getCommand();
                return nested.undoAsNestedCommand(ParallelMacroCommand.this, context);
            } catch (Exception e) {
                context.failed(e);
                getLog().error("Rollback failed", e);
                return context;
            } finally {
                latch.countDown();
            }
        }
    }
}
