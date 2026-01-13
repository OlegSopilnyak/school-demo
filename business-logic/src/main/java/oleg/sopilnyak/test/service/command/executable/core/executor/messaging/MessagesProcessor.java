package oleg.sopilnyak.test.service.command.executable.core.executor.messaging;

import static oleg.sopilnyak.test.service.message.CommandMessage.EMPTY;

import oleg.sopilnyak.test.service.message.CommandMessage;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.slf4j.Logger;

/**
 * Processor: to process command-message in command-though-message service
 *
 * @see oleg.sopilnyak.test.service.message.CommandMessage
 * @see CommandThroughMessagesExecutor
 */
public interface MessagesProcessor {
    // predicate to check is taken message last one
    Predicate<CommandMessage<?>> IS_LAST_MESSAGE = message -> Objects.equals(message, EMPTY);

    /**
     * Get the name of the processor for logging purposes.
     *
     * @return the name of the processor
     */
    String getProcessorName();

    /**
     * To check if the processor's owner (service) is active
     *
     * @return true if the processor's owner is active
     * @see CommandThroughMessagesExecutor
     */
    boolean isOwnerActive();

    /**
     * To take command-message from the appropriate messages processor's source for further processing
     *
     * @param <T> command execution result type
     * @return the command message taken from the appropriate processor's source
     * @throws InterruptedException if interrupted while waiting
     * @see CommandMessage
     */
    <T> CommandMessage<T> takeMessage() throws InterruptedException;

    /**
     * To check is there any active messages to process
     *
     * @return true if processor is waiting for the message
     * @see this#accept(CommandMessage)
     */
    boolean isEmpty();

    /**
     * To accept for command-message's processing
     *
     * @param <T>     command execution result type
     * @param message command-message to process
     * @return true, if message is accepted for the command-message's processing, false otherwise
     */
    <T> boolean accept(CommandMessage<T> message);

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
     * To process the taken message.
     *
     * @param message the command message to be processed
     */
    void onTakenMessage(CommandMessage<?> message);

    /**
     * To run processor's taken message in asynchronous way
     *
     * @param onMessageAction consumer of taken message to process
     * @param taken taken message instance
     */
    void runAsyncTakenMessage(Consumer<CommandMessage<?>> onMessageAction, CommandMessage<?> taken);

    /**
     * To execute command-messages' processor main loop
     */
    default void doingMainLoop() {
        // checking processor's state
        if (isProcessorActive()) {
            getLogger().warn("{} is already working.", getProcessorName());
            return;
        }
        //
        // process isn't active
        // making processor active
        activateProcessor();
        getLogger().info("{} is started. Main service active = '{}'", getProcessorName(), isOwnerActive());
        //
        // main processor loop
        while (isOwnerActive()) try {
            //
            // taking the message depends on processor's implementation
            final CommandMessage<?> message = takeMessage();
            //
            // check service-owner state or last message taken
            if (!isOwnerActive() || IS_LAST_MESSAGE.test(message)) {
                getLogger().debug(!isOwnerActive()
                                // the processor's owner stops the processor
                                ? "{} is going to stop by owner request."
                                // last message is received
                                : "Received the last message, the processor {} is going to stop.",
                        getProcessorName()
                );
                break;
            } else {
                //
                // process the command-message asynchronously
                runAsyncTakenMessage(this::onTakenMessage, message);
                getLogger().debug(
                        "The processor {} runs received message processing in asynchronous way.",
                        getProcessorName()
                );
            }
        } catch (InterruptedException e) {
            getLogger().warn("{} getting command requests is interrupted", getProcessorName(), e);
            /* Clean up whatever needs to be handled before interrupting  */
            Thread.currentThread().interrupt();
            // to leave the main loop
            break;
        } catch (Exception e) {
            getLogger().warn("{} getting command requests throws", getProcessorName(), e);
            // to leave the main loop
            break;
        }
        // mark processor as inactive
        deActivateProcessor();
    }

    /**
     * To shut down the messages processor
     */
    void shutdown();

    /**
     * To get access to the logger of the processor
     *
     * @return logger's reference
     */
    Logger getLogger();
}
