package oleg.sopilnyak.test.service.facade.impl.command.message.service.local;

import oleg.sopilnyak.test.service.facade.impl.command.message.MessagesProcessorAdapter;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandMessage;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import org.slf4j.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;

/**
 *  Processor: parent class of messages processing processor, using local blocking-queue
 */
abstract class MessagesProcessorOnLocalBlockingQueue extends MessagesProcessorAdapter {
    // last message in the queue marker
    private static final String LAST_MESSAGE = "Last Message Marker";
    private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
    @Setter
    protected ObjectMapper objectMapper;

    protected MessagesProcessorOnLocalBlockingQueue(
            AtomicBoolean processorActive, AtomicBoolean serviceActive, Executor executor, Logger log
    ) {
        super(processorActive, serviceActive, executor, log);
    }

    /**
     * To accept for processing command-message
     *
     * @param message command-message to process
     */
    @Override
    public <T> boolean accept(final CommandMessage<T> message) {
        log.debug("Put to the queue command message {}", message);
        try {
            final String stringMessage = CommandMessage.EMPTY.equals(message)
                    // The last message marker
                    ? LAST_MESSAGE
                    // serialize message to JSON
                    : objectMapper.writeValueAsString(message);
            log.debug("Put to the queue command message {}", stringMessage);
            return messages.add(stringMessage);
        } catch (IOException e) {
            log.warn("Failed to serialize message to json", e);
        }
        return false;
    }

    /**
     * To take command message from the appropriate messages queue for further processing
     *
     * @return the command message taken from the appropriate queue
     * @throws InterruptedException if interrupted while waiting
     * @see CommandMessage
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> CommandMessage<T> takeMessage() throws InterruptedException {
        final Predicate<String> isLast = msg -> msg.isBlank() || LAST_MESSAGE.equals(msg);
        log.debug("Taking available command message from the queue.");
        final String takenMessage = messages.take();
        log.debug("Took from the queue command message {}", takenMessage);
        if (isLast.test(takenMessage)) {
            return (CommandMessage<T>) CommandMessage.EMPTY;
        }
        // try to deserialize command-message from JSON
        try {
            return objectMapper.readValue(takenMessage, BaseCommandMessage.class);
        } catch (IOException e) {
            log.error("Failed to deserialize the message", e);
            return (CommandMessage<T>) CommandMessage.EMPTY;
        } catch (Exception e) {
            log.error("Something went wrong during deserialization of message", e);
            return (CommandMessage<T>) CommandMessage.EMPTY;
        }
    }

    /**
     * To check are there are available messages to process
     *
     * @return true if processor is waiting for the message
     */
    @Override
    public boolean isEmpty() {
        return messages.isEmpty();
    }
}
