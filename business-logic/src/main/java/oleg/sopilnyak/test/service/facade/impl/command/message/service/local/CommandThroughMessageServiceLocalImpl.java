package oleg.sopilnyak.test.service.facade.impl.command.message.service.local;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.MessageProgressWatchdog;
import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.MessagesProcessor;
import oleg.sopilnyak.test.service.facade.impl.command.message.MessagesProcessorAdapter;
import oleg.sopilnyak.test.service.facade.impl.command.message.service.CommandThroughMessageServiceAdapter;
import oleg.sopilnyak.test.service.message.CommandMessage;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

/**
 * Service Implementation: execute command using request/response model (Local version through blocking queues)
 *
 * @see CommandThroughMessageService
 * @see CommandActionExecutor#processActionCommand(CommandMessage)
 * @see BlockingQueue
 */

@Slf4j
@Service
@Deprecated
public class CommandThroughMessageServiceLocalImpl extends CommandThroughMessageServiceAdapter {
    // Flags to control the current state of the processors
    private static final Map<Class<? extends MessagesProcessor>, AtomicBoolean> processorStates = Map.of(
            RequestsProcessor.class, new AtomicBoolean(false),
            ResponsesProcessor.class, new AtomicBoolean(false)
    );
    // monitor to shut down message-processors properly
    private final Object processorStatesMonitor = new Object();

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
        final var processor = new RequestsProcessor(processorStates.get(RequestsProcessor.class), serviceActive, executor, log);
        processor.setObjectMapper(objectMapper);
        return processor;
    }

    @Override
    protected MessagesProcessorAdapter prepareOutputProcessor(final AtomicBoolean serviceActive, final Executor executor) {
        final var processor = new ResponsesProcessor(processorStates.get(ResponsesProcessor.class), serviceActive, executor, log);
        processor.setObjectMapper(objectMapper);
        return processor;
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

    // inner classes
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

        /**
         * To process the taken message.
         *
         * @param message the command message to be processed
         */
        @Override
        public void onTakenMessage(final CommandMessage<?> message) {
            // setting up processing context for the working thread
            ActionContext.install(message.getActionContext());
            try {
                log.debug("Starting request's processing with direction:{} correlation-id:{}",
                        message.getDirection(), message.getCorrelationId());
                // process taken message in the data transaction
                processTakenRequestCommandMessage(message);
                log.debug(" ++ Successfully processed request with direction:{} correlation-id:{}",
                        message.getDirection(), message.getCorrelationId());
            } catch (Throwable e) {
                // process message after the error has thrown
                log.error("== Couldn't process message request with correlation-id:{}", message.getCorrelationId(), e);
                processUnprocessedRequestCommandMessage(message, e);
            } finally {
                // release current processing context
                ActionContext.release();
            }
        }

        /**
         * To run processor's taken message in asynchronous way
         *
         * @param runProcessTakenMessage taken message process runner
         */
        @Override
        public void runAsyncTakenMessage(Runnable runProcessTakenMessage) {

        }

        /**
         * To shut down the messages processor
         */
        @Override
        public void shutdown() {

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

        /**
         * To run processor's taken message in asynchronous way
         *
         * @param runProcessTakenMessage taken message process runner
         */
        @Override
        public void runAsyncTakenMessage(Runnable runProcessTakenMessage) {

        }

        /**
         * To shut down the messages processor
         */
        @Override
        public void shutdown() {

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
