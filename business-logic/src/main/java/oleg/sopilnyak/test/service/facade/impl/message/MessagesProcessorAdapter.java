package oleg.sopilnyak.test.service.facade.impl.message;

import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandMessage;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;

/**
 * Processor: parent of messages processor for requests and response messages
 */
public abstract class MessagesProcessorAdapter {
    protected final Logger log;
    private final AtomicBoolean processorActive;
    private final AtomicBoolean serviceActive;
    private final Executor executor;

    protected MessagesProcessorAdapter(AtomicBoolean processorActive,
                                       AtomicBoolean serviceActive,
                                       Executor executor,
                                       Logger log) {
        this.processorActive = processorActive;
        this.serviceActive = serviceActive;
        this.executor = executor;
        this.log = log;
    }

    /**
     * To execute messages processor business-logic
     */
    public void processing() {
        if (isProcessorActive()) {
            log.warn("{} is already running.", getProcessorName());
            return;
        }
        // making it active
        activateProcessor();
        log.info("{} is started. Main service active = '{}'", getProcessorName(), serviceActive);
        while (serviceActive.get()) {
            try {
                final CommandMessage<?> message = takeFromQueue();
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
    public abstract String getProcessorName();

    /**
     * To apply for processing command-message
     *
     * @return true, if message is accepted for the processing, false otherwise
     * @param message command-message to process
     * @param <T> command execution result type
     */
    public abstract <T> boolean apply(CommandMessage<T> message);

    /**
     * To check are there are active messages to process
     *
     * @return true if processor is waiting for the message
     */
    public abstract boolean isEmpty();

    /**
     * To take command message from the appropriate queue for further processing
     *
     * @return the command message taken from the appropriate queue
     * @throws InterruptedException if interrupted while waiting
     * @see BaseCommandMessage
     */
    protected abstract <T> CommandMessage<T> takeFromQueue() throws InterruptedException;

    /**
     * To process the taken message.
     *
     * @param message the command message to be processed
     */
    protected abstract void processTakenMessage(CommandMessage<?> message);

    /**
     * To check if the processor is active
     *
     * @return true if the processor is active
     */
    public boolean isProcessorActive() {
        return processorActive.get();
    }

    /**
     * To activate the processor
     */
    private void activateProcessor(){
        processorActive.getAndSet(true);
    }

    /**
     * To deactivate the processor
     */
    private void deActivateProcessor(){
        processorActive.getAndSet(false);
    }
}
