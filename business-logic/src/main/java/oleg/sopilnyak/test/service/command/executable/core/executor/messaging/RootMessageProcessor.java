package oleg.sopilnyak.test.service.command.executable.core.executor.messaging;

import static oleg.sopilnyak.test.service.message.CommandMessage.EMPTY;

import oleg.sopilnyak.test.service.message.CommandMessage;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Processor: possible parent processor of some command-messages processor for requests and response messages flows
 */
@SuperBuilder
public abstract class RootMessageProcessor implements MessagesProcessor {
    protected final Logger logger;
    protected final MessagesExchange exchange;
    @Builder.Default
    @Getter
    private String processorName = "== Unknown MessageProcessor ==";
    @Builder.Default
    private Consumer<CommandMessage<?>> processingTaken = message -> LoggerFactory
            .getLogger("<= Default-Taken-Message-Processing =>")
            .error("There's no defined processing for message: {}", message);
    // processor state holder
    private final AtomicBoolean processorActive = new AtomicBoolean(false);
    // monitor to shut down messages-processor properly
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
        return logger;
    }

    /**
     * To run processor's taken message in asynchronous way
     * In root processor implementation there is no any threads involved
     *
     * @param runnableForTakenMessage taken message process runner
     */
    @Override
    public void runAsyncTakenMessage(final Runnable runnableForTakenMessage) {
        runnableForTakenMessage.run();
    }

    /**
     * To process the taken message.
     *
     * @param message the command message to be processed
     */
    @Override
    public void onTakenMessage(final CommandMessage<?> message) {
        getLogger().debug("Taken message {}", message);
        processingTaken.accept(message);
    }

    /**
     * To shut down the command-messages processor
     */
    @Override
    public void shutdown() {
        // put the empty message as a last one to the processor
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
