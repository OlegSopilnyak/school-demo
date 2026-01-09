package oleg.sopilnyak.test.service.command.executable.core.executor.messaging.local;

import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.MessagesProcessor;
import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.RootMessageProcessor;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandMessage;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Predicate;
import org.springframework.util.ObjectUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Processor: command-messages processor for requests and response messages flows based on local blocking-queue
 *
 * @see BlockingQueue
 */
@SuperBuilder
abstract class LocalQueueMessageProcessor extends RootMessageProcessor {
    // Executors for asynchronous taken messages processing
    private final Executor executor;
    // the predicate to check taken message's emptiness
    private static final Predicate<String> IS_EMPTY_MESSAGE = json -> ObjectUtils.isEmpty(json) || json.isBlank();
    // last message in the queue marker
    private static final String LAST_MESSAGE = "Last Message Marker";
    private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
    @Setter
    protected ObjectMapper objectMapper;

    /**
     * To run processor's taken command-message in asynchronous way <BR/>
     * Local processor use CompletableFuture feature
     *
     * @param runnableForTakenMessage taken message process runner
     * @see CompletableFuture#runAsync(Runnable, Executor)
     */
    @Override
    public void runAsyncTakenMessage(final Runnable runnableForTakenMessage) {
        CompletableFuture.runAsync(runnableForTakenMessage, executor);
    }

    /**
     * To take command-message from the appropriate messages processor's source for further processing in the processor
     *
     * @return the command message taken from the appropriate processor's source
     * @throws InterruptedException if interrupted while waiting
     * @see CommandMessage
     * @see MessagesProcessor#doingMainLoop()
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> CommandMessage<T> takeMessage() throws InterruptedException {
        logger.debug("Taking available command message from the queue.");
        final String takenMessageJson = messages.take();
        logger.debug("Took from the queue command message {}", takenMessageJson);
        if (IS_EMPTY_MESSAGE.or(LAST_MESSAGE::equals).test(takenMessageJson)) {
            return (CommandMessage<T>) CommandMessage.EMPTY;
        }
        // try to deserialize command-message from JSON
        try {
            return objectMapper.readValue(takenMessageJson, BaseCommandMessage.class);
        } catch (IOException e) {
            logger.error("Failed deserialization of command-message", e);
            return (CommandMessage<T>) CommandMessage.EMPTY;
        } catch (Exception e) {
            logger.error("Something went wrong during deserialization of the command-message", e);
            return (CommandMessage<T>) CommandMessage.EMPTY;
        }
    }

    /**
     * To check is there any active messages to process
     *
     * @return true if processor is waiting for the message
     * @see this#accept(CommandMessage)
     */
    @Override
    public boolean isEmpty() {
        return messages.isEmpty();
    }

    /**
     * To accept for doingMainLoop command-message
     *
     * @param message command-message to process
     * @return true, if message is accepted for the doingMainLoop, false otherwise
     */
    @Override
    public <T> boolean accept(final CommandMessage<T> message) {
        logger.debug("Put to the queue command message {}", message);
        try {
            final String stringMessage = CommandMessage.EMPTY.equals(message)
                    // The last message marker
                    ? LAST_MESSAGE
                    // serialize message to JSON
                    : objectMapper.writeValueAsString(message);
            logger.debug("Put to the queue command message {}", stringMessage);
            return messages.add(stringMessage);
        } catch (IOException e) {
            logger.warn("Failed to serialize message to json", e);
            message.getContext().failed(e);
        }
        return false;
    }
}
