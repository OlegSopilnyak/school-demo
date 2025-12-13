package oleg.sopilnyak.test.service.facade.impl.command.message;

import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandMessage;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import org.slf4j.Logger;

/**
 * Processor: to process command-message in command-though-message service
 *
 * @see oleg.sopilnyak.test.service.message.CommandMessage
 * @see oleg.sopilnyak.test.service.message.CommandThroughMessageService
 */
public interface MessagesProcessor {
    /**
     * Get the name of the processor for logging purposes.
     *
     * @return the name of the processor
     */
    String getProcessorName();

    /**
     * To execute messages processor main business-logic
     */
    default void processing() {
        if (isProcessorActive()) {
            getLogger().warn("{} is already running.", getProcessorName());
            return;
        }
        // making it active
        activateProcessor();
        getLogger().info("{} is started. Main service active = '{}'", getProcessorName(), isOwnerActive());
        final Predicate<CommandMessage<?>> lastMessage = msg -> Objects.equals(msg, BaseCommandMessage.EMPTY);
        while (isOwnerActive()) {
            try {
                final CommandMessage<?> message = takeMessage();
                if (!isOwnerActive()) {
                    getLogger().debug("{} is going to stop.", getProcessorName());
                    break;
                }
                // testing taken message
                if (lastMessage.test(message)) {
                    // last message is received
                    getLogger().info("Received the last message, the processor {} is going to stop.", getProcessorName());
                    break;
                } else {
                    // process the message asynchronously if service is active and message isn't empty
                    CompletableFuture.runAsync(() -> onTakenMessage(message), takenMessageExecutor());
                    getLogger().info("The processor {} launches received message processing in separate thread.", getProcessorName());
                }
            } catch (InterruptedException e) {
                getLogger().warn("{} getting command requests is interrupted", getProcessorName(), e);
                /* Clean up whatever needs to be handled before interrupting  */
                Thread.currentThread().interrupt();
            }
        }
        // mark as inactive
        deActivateProcessor();
    }

    /**
     * To check if the processor's owner (service) is active
     *
     * @return true if the processor's owner is active
     * @see oleg.sopilnyak.test.service.message.CommandThroughMessageService
     */
    boolean isOwnerActive();

    /**
     * To take command message from the appropriate messages processor's source for further processing
     *
     * @param <T> command execution result type
     * @return the command message taken from the appropriate processor's source
     * @throws InterruptedException if interrupted while waiting
     * @see CommandMessage
     */
    <T> CommandMessage<T> takeMessage() throws InterruptedException;

    /**
     * To get the executor for taken messages
     *
     * @return executor reference
     * @see this#onTakenMessage(CommandMessage)
     */
    Executor takenMessageExecutor();

    /**
     * To check is there any active messages to process
     *
     * @return true if processor is waiting for the message
     * @see this#accept(CommandMessage)
     */
    boolean isEmpty();

    /**
     * To accept for processing command-message
     *
     * @param <T>     command execution result type
     * @param message command-message to process
     * @return true, if message is accepted for the processing, false otherwise
     */
    <T> boolean accept(CommandMessage<T> message);

    /**
     * To process the taken message.
     *
     * @param message the command message to be processed
     */
    void onTakenMessage(CommandMessage<?> message);

    /**
     * To check if the processor is active
     *
     * @return true if the processor is active
     */
    boolean isProcessorActive();

    /**
     * To change processor's state
     *
     * @param state new state value
     */
    void setProcessorActive(boolean state);

    /**
     * To activate the processor
     * Change the processor-active state to true
     *
     * @see this#isProcessorActive()
     * @see this#setProcessorActive(boolean)
     */
    default void activateProcessor() {
        setProcessorActive(true);
    }

    /**
     * To deactivate the processor
     * Change the processor-active state to false
     *
     * @see this#isProcessorActive()
     * @see this#setProcessorActive(boolean)
     */
    default void deActivateProcessor() {
        setProcessorActive(false);
    }

    /**
     * To get access to the logger of the processor
     *
     * @return logger's reference
     */
    Logger getLogger();
}
