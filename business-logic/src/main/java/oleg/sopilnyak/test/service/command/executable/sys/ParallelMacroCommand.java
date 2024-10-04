package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.exception.CountDownLatchInterruptedException;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import org.slf4j.Logger;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.util.ObjectUtils;

import java.util.Deque;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * Parallel MacroCommand: macro-command the command with nested commands inside.
 * Uses execution of nested commands simultaneously
 */
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
        if (ObjectUtils.isEmpty(doContexts)) {
            getLog().warn("Nothing to do");
            return;
        }
        final int actionsQuantity = doContexts.size();
        // Queue for nested command actions
        final BlockingQueue<DoInRootTransaction<T>> actionsQueue = new LinkedBlockingQueue<>(actionsQuantity);
        final CountDownLatch latch = new CountDownLatch(actionsQuantity);
        // parallel walking through contexts set
        doContexts.forEach(context -> kickOffDoRunner(actionsQueue, context, stateListener, latch));
        // process actions from other threads
        processActionsQueue(actionsQueue, actionsQuantity);

        // waiting for CountDownLatch latch exceed
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
    public <T> Deque<Context<T>> undoNestedCommands(final Deque<Context<T>> doneContexts) {
        if (ObjectUtils.isEmpty(doneContexts)) {
            getLog().warn("Nothing to undo");
            return doneContexts;
        }
        final int actionsQuantity = doneContexts.size();
        // Queue for nested command actions
        final BlockingQueue<DoInRootTransaction<T>> actionsQueue = new LinkedBlockingQueue<>(actionsQuantity);
        final CountDownLatch latch = new CountDownLatch(actionsQuantity);
        // parallel walking through contexts set
        doneContexts.forEach(context -> kickOffUndoRunner(actionsQueue, context, latch));
        // process actions from other threads
        processActionsQueue(actionsQueue, actionsQuantity);

        // waiting for CountDownLatch latch exceed
        try {
            latch.await();
        } catch (InterruptedException e) {
            processInterruptedException(e, latch);
        }
        return doneContexts;
    }

    // private methods
    private <T> void processActionsQueue(final BlockingQueue<DoInRootTransaction<T>> actionsQueue, final int capacity) {
        IntStream.range(0, capacity).forEach(i -> {
            try {
                actionsQueue.take().actionInRootTransaction();
            } catch (InterruptedException e) {
                getLog().error("Interrupted", e);
                throw new UnableExecuteCommandException("unknown", e);
            }
        });
    }

    private void processInterruptedException(InterruptedException e, CountDownLatch latch) {
        getLog().error("CountDownLatch is interrupted", e);
        // Clean up whatever needs to be handled before interrupting
        Thread.currentThread().interrupt();
        throw new CountDownLatchInterruptedException(latch.getCount(), e);
    }

    private <T> void kickOffDoRunner(final BlockingQueue<DoInRootTransaction<T>> actionsQueue,
                                     final Context<T> context,
                                     final Context.StateChangedListener<T> stateListener,
                                     final CountDownLatch latch) {
        getLog().debug("Submit executing of command: '{}' with context:{}", context.getCommand().getId(), context);
        final DoCommandRunner<T> runner = new DoCommandRunner<>(actionsQueue, context, stateListener, latch);
        final Future<Context<T>> future = getExecutor().submit(runner);

        // To test is doRunner starting well in commandContextExecutor
        if (future.isCancelled()) {
            getLog().warn("Canceled executing of command: '{}' with context:{}", context.getCommand().getId(), context);
            context.addStateListener(stateListener);
            context.setState(Context.State.CANCEL);
            context.removeStateListener(stateListener);
            latch.countDown();
        }
    }

    private <T> void kickOffUndoRunner(final BlockingQueue<DoInRootTransaction<T>> actionsQueue,
                                       final Context<T> context,
                                       final CountDownLatch latch) {
        getLog().debug("Submit rolling back of command: '{}' with context:{}", context.getCommand().getId(), context);
        final UndoCommandRunner<T> runner = new UndoCommandRunner<>(actionsQueue, context, latch);
        final Future<Context<T>> future = getExecutor().submit(runner);

        // To test is undoRunner starting well in commandContextExecutor
        if (future.isCancelled()) {
            getLog().warn("Canceled rolling back of command: '{}' with context:{}", context.getCommand().getId(), context);
            context.setState(Context.State.CANCEL);
            latch.countDown();
        }
    }

    // private classes methods
    private static class DoInRootTransaction<T> {
        final Object actionFinished = new Object();
        volatile boolean actionInProgress = true;
        final Consumer<T> toDo;
        final Logger logger;

        DoInRootTransaction(Consumer<T> toDo, Logger logger) {
            this.toDo = toDo;
            this.logger = logger;
        }

        void actionInRootTransaction() {
            try {
                toDo.accept(null);
            } catch (Exception e) {
                logger.error("Cannot execute action.", e);
            } finally {
                actionIsFinished();
            }
        }

        void actionIsFinished() {
            actionInProgress = false;
            synchronized (actionFinished) {
                actionFinished.notifyAll();
            }
        }
    }

    private class DoCommandRunner<T> extends InRootTransaction<T> implements Callable<Context<T>> {
        final Context<T> context;
        final Context.StateChangedListener<T> stateListener;
        final CountDownLatch latch;

        public DoCommandRunner(final BlockingQueue<DoInRootTransaction<T>> actionsQueue,
                               final Context<T> context,
                               final Context.StateChangedListener<T> stateListener,
                               final CountDownLatch latch) {
            super(actionsQueue);
            this.context = context;
            this.stateListener = stateListener;
            this.latch = latch;
        }

        @Override
        public Context<T> call() {
            final Consumer<T> action = t ->
                    context.getCommand().doAsNestedCommand(ParallelMacroCommand.this, context, stateListener);
            runActionAndWait(action);
            latch.countDown();
            return context;
        }
    }

    private class UndoCommandRunner<T> extends InRootTransaction<T> implements Callable<Context<T>> {
        final Context<T> context;
        final CountDownLatch latch;

        public UndoCommandRunner(final BlockingQueue<DoInRootTransaction<T>> actionsQueue,
                                 final Context<T> context,
                                 final CountDownLatch latch) {
            super(actionsQueue);
            this.context = context;
            this.latch = latch;
        }

        @Override
        public Context<T> call() {
            final Consumer<T> action = t ->
                    context.getCommand().undoAsNestedCommand(ParallelMacroCommand.this, context);
            runActionAndWait(action);
            latch.countDown();
            return context;
        }
    }

    protected abstract class InRootTransaction<T> {
        final BlockingQueue<DoInRootTransaction<T>> actionsQueue;

        protected InRootTransaction(final BlockingQueue<DoInRootTransaction<T>> actionsQueue) {
            this.actionsQueue = actionsQueue;
        }

        protected void runActionAndWait(Consumer<T> action) {
            final DoInRootTransaction<T> inRootTransaction = new DoInRootTransaction<>(action, getLog());
            if (actionsQueue.offer(inRootTransaction)) {
                synchronized (inRootTransaction.actionFinished) {
                    try {
                        while (inRootTransaction.actionInProgress) {
                            inRootTransaction.actionFinished.wait();
                        }
                    } catch (InterruptedException e) {
                        getLog().error("Interrupted", e);
                        Thread.currentThread().interrupt();
                    }
                }
            } else {
                getLog().warn("Cannot add action to the queue");
            }
        }
    }
}
