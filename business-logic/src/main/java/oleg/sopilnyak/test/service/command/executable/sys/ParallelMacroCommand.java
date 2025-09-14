package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.io.Input;
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
 *
 * @param <T> the type of command execution (do) result
 * @see MacroCommand
 * @see SchedulingTaskExecutor
 */
public abstract class ParallelMacroCommand<T> extends MacroCommand<T> {
    protected ParallelMacroCommand(ActionExecutor actionExecutor) {
        super(actionExecutor);
    }

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
    public Deque<Context<?>> executeNested(final Deque<Context<?>> doContexts, final Context.StateChangedListener stateListener) {
        if (ObjectUtils.isEmpty(doContexts)) {
            getLog().warn("Nothing to do");
            return doContexts;
        }
        final int actionsQuantity = doContexts.size();
        // Queue for nested command actions
        final BlockingQueue<DoInRootTransaction> actionsQueue = new LinkedBlockingQueue<>(actionsQuantity);
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
        return doContexts;
    }


    /**
     * To rollback changes for contexts with state DONE<BR/>
     * sequential revers order of commands deque
     *
     * @param inputDoneContexts wrapped collection of contexts with DONE state
     * @see SchedulingTaskExecutor#submit(Callable)
     * @see Deque
     * @see Context.State#DONE
     */
    @Override
    public Deque<Context<?>> rollbackNestedDone(final Input<Deque<Context<?>>> inputDoneContexts) {
        final Deque<Context<?>> doneContexts = inputDoneContexts.value();
        if (ObjectUtils.isEmpty(doneContexts)) {
            getLog().warn("Nothing to undo");
            return doneContexts;
        }
        final int actionsQuantity = doneContexts.size();
        // Queue for nested command actions
        final BlockingQueue<DoInRootTransaction> actionsQueue = new LinkedBlockingQueue<>(actionsQuantity);
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
    private void processActionsQueue(final BlockingQueue<DoInRootTransaction> actionsQueue, final int capacity) {
        IntStream.range(0, capacity).forEach(i -> {
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

    private void processInterruptedException(InterruptedException e, CountDownLatch latch) {
        getLog().error("CountDownLatch is interrupted", e);
        // Clean up whatever needs to be handled before interrupting
        Thread.currentThread().interrupt();
        throw new CountDownLatchInterruptedException(latch.getCount(), e);
    }

    private void kickOffDoRunner(final BlockingQueue<DoInRootTransaction> actionsQueue,
                                 final Context<?> context, final Context.StateChangedListener stateListener,
                                 final CountDownLatch latch) {
        getLog().debug("Submit executing of command: '{}' with context:{}", context.getCommand().getId(), context);
        final var future = getExecutor().submit(new DoNestedCommandRunner(actionsQueue, context, stateListener, latch));

        // To test is doRunner starting well in commandContextExecutor
        if (future.isCancelled()) {
            getLog().warn("Canceled executing of command: '{}' with context:{}", context.getCommand().getId(), context);
            context.addStateListener(stateListener);
            context.setState(Context.State.CANCEL);
            context.removeStateListener(stateListener);
            latch.countDown();
        }
    }

    private void kickOffUndoRunner(final BlockingQueue<DoInRootTransaction> actionsQueue,
                                   final Context<?> context,
                                   final CountDownLatch latch) {
        getLog().debug("Submit rolling back of command: '{}' with context:{}", context.getCommand().getId(), context);
        final Future<Context<?>> future =
                getExecutor().submit(new UndoNestedCommandRunner(actionsQueue, context, latch));

        // To test is undoRunner starting well in commandContextExecutor
        if (future.isCancelled()) {
            getLog().warn("Canceled rolling back of command: '{}' with context:{}", context.getCommand().getId(), context);
            context.setState(Context.State.CANCEL);
            latch.countDown();
        }
    }

    // private classes methods
    private class DoNestedCommandRunner extends InRootTransaction implements Callable<Context<?>> {
        final Context<?> context;
        final Context.StateChangedListener stateListener;
        final CountDownLatch latch;

        public DoNestedCommandRunner(final BlockingQueue<DoInRootTransaction> actionsQueue,
                                     final Context<?> context, final Context.StateChangedListener stateListener,
                                     final CountDownLatch latch) {
            super(actionsQueue);
            this.context = context;
            this.stateListener = stateListener;
            this.latch = latch;
        }

        @Override
        public Context<?> call() {
            runActionAndWait(action ->
                    context.getCommand().doAsNestedCommand(ParallelMacroCommand.this, context, stateListener)
            );
            latch.countDown();
            return context;
        }
    }

    private class UndoNestedCommandRunner extends InRootTransaction implements Callable<Context<?>> {
        final Context<?> context;
        final CountDownLatch latch;

        public UndoNestedCommandRunner(final BlockingQueue<DoInRootTransaction> actionsQueue,
                                       final Context<?> context, final CountDownLatch latch) {
            super(actionsQueue);
            this.context = context;
            this.latch = latch;
        }

        @Override
        public Context<?> call() {
            runActionAndWait(action ->
                    context.getCommand().undoAsNestedCommand(ParallelMacroCommand.this, context)
            );
            latch.countDown();
            return context;
        }
    }

    protected abstract class InRootTransaction {
        final BlockingQueue<DoInRootTransaction> actionsQueue;

        protected InRootTransaction(final BlockingQueue<DoInRootTransaction> actionsQueue) {
            this.actionsQueue = actionsQueue;
        }

        protected void runActionAndWait(Consumer<?> action) {
            final DoInRootTransaction inRootTransaction = new DoInRootTransaction(action, getLog());
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

    static final class DoInRootTransaction {
        final Object actionFinished = new Object();
        volatile boolean actionInProgress = true;
        final Consumer<?> toDo;
        final Logger logger;

        DoInRootTransaction(Consumer<?> toDo, Logger logger) {
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
}
