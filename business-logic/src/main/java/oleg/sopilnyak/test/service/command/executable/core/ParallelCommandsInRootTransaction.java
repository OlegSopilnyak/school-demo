package oleg.sopilnyak.test.service.command.executable.core;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import lombok.ToString;

/**
 * To execute commands in one transaction through the actions queue
 * @deprecated
 */
@Deprecated
abstract class ParallelCommandsInRootTransaction<T> {
    private final BlockingQueue<ActionInRootTransaction<T>> actionsQueue;

    protected ParallelCommandsInRootTransaction(final BlockingQueue<ActionInRootTransaction<T>> actionsQueue) {
        this.actionsQueue = actionsQueue;
    }

    protected T runActionAndWait(Supplier<T> action) {
        final ActionInRootTransaction<T> inRootTransaction = new ActionInRootTransaction<>(action, getLog());
        if (actionsQueue.offer(inRootTransaction)) {
            synchronized (inRootTransaction.actionFinishedMonitor) {
                try {
                    while (inRootTransaction.actionInProgress.get()) {
                        inRootTransaction.actionFinishedMonitor.wait();
                    }
                } catch (InterruptedException e) {
                    getLog().error("Interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }
            return inRootTransaction.result.get();
        } else {
            getLog().warn("Cannot add action to the queue");
            return null;
        }
    }

    abstract Logger getLog();

    // inner classes
    // class to provide execution transactional activity in the separate thread
    @ToString
    static class ActionInRootTransaction<C> {
        private final AtomicBoolean actionInProgress = new AtomicBoolean(true);
        private final AtomicReference<C> result = new AtomicReference<>(null);
        @ToString.Exclude
        private final Object actionFinishedMonitor = new Object();
        @ToString.Exclude
        private final Supplier<C> calculateResult;
        @ToString.Exclude
        private final Logger logger;

        ActionInRootTransaction(Supplier<C> calculateResult, Logger logger) {
            this.calculateResult = calculateResult;
            this.logger = logger;
        }

        void actionInRootTransaction() {
            try {
                result.getAndSet(calculateResult.get());
            } catch (Exception e) {
                logger.error("Cannot execute action.", e);
            } finally {
                actionIsFinished();
            }
        }

        void actionIsFinished() {
            actionInProgress.compareAndSet(true, false);
            synchronized (actionFinishedMonitor) {
                actionFinishedMonitor.notifyAll();
            }
        }
    }
}
