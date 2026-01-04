package oleg.sopilnyak.test.service.command.executable.core.executor.messaging;

import static oleg.sopilnyak.test.service.message.CommandMessage.EMPTY;

import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import lombok.experimental.SuperBuilder;

/**
 * Processor: possible parent processor of some command-messages processor for requests and response messages flows
 */
@SuperBuilder
public abstract class RootMessageProcessor implements MessagesProcessor {
    protected final Logger log;
    protected final MessagesExchange exchange;
    // processor state holder
    private final AtomicBoolean processorActive = new AtomicBoolean(false);
    // monitor to shut down message-processors properly
    private final Object processorStateMonitor = new Object();

    /**
     * To check if the processor's owner (service) is active
     *
     * @return true if the command-message processor's owner is active
     * @see MessagesExchange#isActive()
     */
    @Override
    public boolean isOwnerActive() {
        return exchange.isActive();
    }

    /**
     * To check if the command-message processor is active
     *
     * @return true if the processor is active
     */
    public boolean isProcessorActive() {
        return processorActive.get();
    }

    /**
     * To change command-message processor's state
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
    /**
     * To shut down the command-messages processor
     */
    @Override
    public void shutdown() {
        // put last message to the processor
        accept(EMPTY);
        // waiting for processor is active
        while (isProcessorActive()) synchronized (processorStateMonitor) {
            try {
                processorStateMonitor.wait(25);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
