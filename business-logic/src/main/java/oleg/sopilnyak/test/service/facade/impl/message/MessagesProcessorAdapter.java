package oleg.sopilnyak.test.service.facade.impl.message;

import oleg.sopilnyak.test.service.message.BaseCommandMessage;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;

/**
 * Processor: parent of messages processor for requests and response messages
 */
public abstract class MessagesProcessorAdapter {
    private final AtomicBoolean serviceActive;
    private final Executor executor;
    private final Logger log;

    protected MessagesProcessorAdapter(AtomicBoolean serviceActive, Executor executor, Logger log) {
        this.serviceActive = serviceActive;
        this.executor = executor;
        this.log = log;
    }

    /**
     * To execute messages processor logic
     */
    public void processing() {
        if (isProcessorActive()) {
            log.warn("{} is already running.", getProcessorName());
            return;
        }
        // mark as active
        activateProcessor();
        log.info("{} is started. Main service active = '{}'", getProcessorName(), serviceActive);
        while (serviceActive.get()) {
            try {
                final BaseCommandMessage<?> message = takeFromQueue();
                if (serviceActive.get() && !Objects.equals(message, BaseCommandMessage.EMPTY)) {
                    // process the message asynchronously if not empty
                    CompletableFuture.runAsync(() -> processTakenMessage(message), executor);
                }
            } catch (InterruptedException e) {
                log.warn("{} getting command requests is interrupted", getProcessorName(), e);
                /* Clean up whatever needs to be handled before interrupting  */
                Thread.currentThread().interrupt();
            }
        }
        // mark as inactive
        deActivateProcessor();
    }

    /**
     * Get the name of the processor for logging purposes.
     *
     * @return the name of the processor
     */
    protected abstract String getProcessorName();

    /**
     * To take command message from the appropriate queue for further processing
     *
     * @return the command message taken from the appropriate queue
     * @throws InterruptedException if interrupted while waiting
     * @see BaseCommandMessage
     */
    protected abstract <T> BaseCommandMessage<T> takeFromQueue() throws InterruptedException;

    /**
     * To process the taken message.
     *
     * @param message the command message to be processed
     */
    protected abstract void processTakenMessage(BaseCommandMessage<?> message);

    /**
     * To check if the processor is active
     *
     * @return true if the processor is active
     */
    protected abstract boolean isProcessorActive();

    /**
     * To activate the processor
     */
    protected abstract void activateProcessor();

    /**
     * To deactivate the processor
     */
    protected abstract void deActivateProcessor();
}
