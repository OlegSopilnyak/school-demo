package oleg.sopilnyak.test.service.facade.impl;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.facade.impl.message.CommandThroughMessageServiceAdapter;
import oleg.sopilnyak.test.service.facade.impl.message.MessageProgressWatchdog;
import oleg.sopilnyak.test.service.facade.impl.message.MessagesProcessor;
import oleg.sopilnyak.test.service.facade.impl.message.MessagesProcessorAdapter;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandMessage;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

/**
 * Service Implementation: execute command using request/response model (Local version through blocking queues)
 *
 * @see CommandThroughMessageService
 * @see ActionExecutor#processActionCommand(CommandMessage)
 * @see BlockingQueue
 */

@Slf4j
@Service
public class CommandThroughMessageServiceLocalImpl extends CommandThroughMessageServiceAdapter {
    // Flags to control the current state of the processors
    private static final Map<Class<? extends MessagesProcessor>, AtomicBoolean> processorStates = Map.of(
            RequestsProcessor.class, new AtomicBoolean(false),
            ResponsesProcessor.class, new AtomicBoolean(false)
    );
    // monitor to shut down message-processors properly
    private final Object processorStatesMonitor = new Object();
    private transient ObjectMapper objectMapper;

    /**
     * Inject customized objects mapper to/from JSON
     *
     * @param objectMapper mapper for transformations
     */
    @Autowired
    public final void setObjectMapper(@Lazy @Qualifier("commandsTroughMessageObjectMapper") ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * To get access to service's logger
     *
     * @return reference to concrete
     */
    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected MessagesProcessorAdapter prepareInputProcessor(final AtomicBoolean serviceActive, final Executor executor) {
        return new RequestsProcessor(processorStates.get(RequestsProcessor.class), serviceActive, executor, log);
    }


    @Override
    protected MessagesProcessorAdapter prepareOutputProcessor(final AtomicBoolean serviceActive, final Executor executor) {
        return new ResponsesProcessor(processorStates.get(ResponsesProcessor.class), serviceActive, executor, log);
    }

    /**
     * To wait for any messages-processor is active
     */
    @Override
    protected void waitForProcessorsAreActive() {
        while (processorStates.values().stream().anyMatch(AtomicBoolean::get)) synchronized (processorStatesMonitor) {
            try {
                processorStatesMonitor.wait(25);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        }
    }


    // private methods

    // inner classes
    // Parent class of messages processing processor, using local blocking-queue
    private abstract class MessagesProcessorOnLocalBlockingQueue extends MessagesProcessorAdapter {
        private final BlockingQueue<CommandMessage<?>> messages = new LinkedBlockingQueue<>();

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
        public <T> boolean accept(CommandMessage<T> message) {
            log.debug("Put to the queue command message {}", message);
            if (!CommandMessage.EMPTY.equals(message)) {
                try {
                    final String json = objectMapper.writeValueAsString(message);
                    log.debug("Put to the queue command message {}", json);
                } catch (JsonProcessingException e) {
                    log.warn("Failed to serialize message to json", e);
                }
            }
            return messages.add(message);
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
            log.debug("Taking available command message from the queue.");
            final CommandMessage<T> takenMessage = (CommandMessage<T>) messages.take();
            if (CommandMessage.EMPTY.equals(takenMessage)) {
                return takenMessage;
            }
            try {
                final String json = objectMapper.writeValueAsString(takenMessage);
                CommandMessage<T> restoredMessage = objectMapper.readValue(json, BaseCommandMessage.class);
                return restoredMessage;
            } catch (Exception e) {
                log.error("Failed to serialize message to json", e);
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

    // Messages processor for background processing of requests
    private class RequestsProcessor extends MessagesProcessorOnLocalBlockingQueue {
        private RequestsProcessor(
                AtomicBoolean processor, AtomicBoolean active, Executor executor, Logger logger
        ) {
            super(processor, active, executor, logger);
        }

        /**
         * Get the name of the processor for logging purposes.
         *
         * @return the name of the processor
         */
        @Override
        public String getProcessorName() {
            return "RequestMessagesProcessor";
        }

        @Override
        public void onTakenMessage(final CommandMessage<?> message) {
            // setting up processing context for current thread
            ActionContext.install(message.getActionContext());
            try {
                log.debug("Start processing request with direction:{} correlation-id:{}",
                        message.getDirection(), message.getCorrelationId());
                // process taken message in the transaction
                processTakenRequestCommandMessage(message);
                log.debug(" ++ Successfully processed request with direction:{} correlation-id:{}",
                        message.getDirection(), message.getCorrelationId());
            } catch (Throwable e) {
                // process message after error has thrown
                log.error("== Couldn't process message request with correlation-id:{}", message.getCorrelationId(), e);
                processUnprocessedRequestCommandMessage(message, e);
            } finally {
                // release current processing context
                ActionContext.release();
            }
        }
    }

    // Messages processor for background processing of responses
    private class ResponsesProcessor extends MessagesProcessorOnLocalBlockingQueue {
        private ResponsesProcessor(AtomicBoolean processor, AtomicBoolean active, Executor executor, Logger logger) {
            super(processor, active, executor, logger);
        }

        /**
         * Get the name of the processor for logging purposes.
         *
         * @return the name of the processor
         */
        @Override
        public String getProcessorName() {
            return "ResponseMessagesProcessor";
        }

        /**
         * To process the taken message.
         *
         * @param message the command message to be processed
         */
        @Override
        public void onTakenMessage(final CommandMessage<?> message) {
            final String correlationId = message.getCorrelationId();
            log.info("Finishing processing response with correlationId='{}' which is needs completion", correlationId);
            completeMessageProcessing(message);
        }

        @SuppressWarnings("unchecked")
        // complete the message processing
        private <T> void completeMessageProcessing(final CommandMessage<T> response) {
            final String correlationId = response.getCorrelationId();
            // check if the message in progress map by correlation-id
            findMessageInProgress(correlationId).ifPresentOrElse(watchDog -> {
                        final MessageProgressWatchdog<T> messageWatcher = (MessageProgressWatchdog<T>) watchDog;
                        // set up the result and mark as completed
                        messageWatcher.setResult(response);
                        // notify thread waiting for this message-watcher result
                        messageWatcher.messageProcessingIsDone();
                        log.info("Successfully processed response with correlationId='{}'", correlationId);
                    },
                    () -> log.warn("= Message with correlationId='{}' is NOT found in progress map", correlationId)
            );
        }
    }
}
