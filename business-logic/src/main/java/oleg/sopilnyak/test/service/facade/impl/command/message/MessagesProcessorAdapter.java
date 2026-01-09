package oleg.sopilnyak.test.service.facade.impl.command.message;

import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.MessagesProcessor;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;

/**
 * Processor: parent of messages processor for requests and response messages
 */
@Deprecated
public abstract class MessagesProcessorAdapter implements MessagesProcessor {
    protected final Logger log;
    private final AtomicBoolean processorActive;
    private final AtomicBoolean serviceActive;

    protected MessagesProcessorAdapter(AtomicBoolean processorActive,
                                       AtomicBoolean serviceActive,
                                       Logger log) {
        this.processorActive = processorActive;
        this.serviceActive = serviceActive;
        this.log = log;
    }

    /**
     * To check if the processor's owner (service) is active
     *
     * @return true if the processor's owner is active
     * @see CommandThroughMessageService
     */
    @Override
    public boolean isOwnerActive() {
        return serviceActive.get();
    }

    /**
     * To check if the processor is active
     *
     * @return true if the processor is active
     */
    public boolean isProcessorActive() {
        return processorActive.get();
    }

    /**
     * To change processor's state
     *
     * @param state new state value
     */
    @Override
    public void setProcessorActive(boolean state) {
        processorActive.getAndSet(state);
    }

    /**
     * To get access to the logger of the processor
     *
     * @return logger's reference
     */
    @Override
    public Logger getLogger() {
        return log;
    }

}
