package oleg.sopilnyak.test.service.command.executable.core.executor.messaging.local;


import static oleg.sopilnyak.test.service.message.CommandThroughMessageService.COMMAND_MESSAGE_OBJECT_MAPPER_BEAN_NAME;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.CommandMessagesExchangeExecutorAdapter;
import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.MessagesProcessor;
import oleg.sopilnyak.test.service.message.CommandMessage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 * Service Implementation: execute command using request/response model (Local version through blocking queues)
 *
 * @see CommandMessagesExchangeExecutorAdapter
 * @see LocalQueueMessageProcessor
 * @see MessagesProcessor#onTakenMessage(CommandMessage)
 */
@Slf4j
@Service
public class LocalQueueCommandExecutor extends CommandMessagesExchangeExecutorAdapter {
    private ExecutorService executor;
    // object mapper for the command-messages transformation and other stuff
    private ObjectMapper objectMapper;

    /**
     * Inject customized objects mapper to/from JSON transformation
     *
     * @param objectMapper the instance of mapper for transformations
     */
    @Autowired
    public final void setObjectMapper(
            @Lazy @Qualifier(COMMAND_MESSAGE_OBJECT_MAPPER_BEAN_NAME) ObjectMapper objectMapper
    ) {
        this.objectMapper = objectMapper;
    }

    /**
     * Build and prepare message-processor for requests messages
     *
     * @return built and prepared messages-processor instance
     * @see CommandMessagesExchangeExecutorAdapter#launchInProcessor(CountDownLatch)
     * @see CommandMessagesExchangeExecutorAdapter#prepareRequestsProcessor()
     */
    @Override
    protected MessagesProcessor prepareRequestsProcessor() {
        return RequestsProcessor.builder()
                .executor(executor).log(log).exchange(this).objectMapper(objectMapper)
                .build();
    }

    /**
     * Build and prepare message-processor for responses messages
     *
     * @return built and prepared messages-processor instance
     * @see CommandMessagesExchangeExecutorAdapter#launchOutProcessor(CountDownLatch)
     * @see CommandMessagesExchangeExecutorAdapter#prepareResponsesProcessor()
     */
    @Override
    protected MessagesProcessor prepareResponsesProcessor() {
        return ResponsesProcessor.builder()
                .executor(executor).log(log).exchange(this).objectMapper(objectMapper)
                .build();
    }

    /**
     * To initialize executor service of the messages taken by message-processor instance
     */
    @Override
    protected void initializeTakenMessagesExecutor() {
        final int corePoolSize = Runtime.getRuntime().availableProcessors();
        executor = Executors.newScheduledThreadPool(corePoolSize, serviceThreadFactory("QueueMessageProcessor-"));
    }

    /**
     * To shut down executor service for messages taken by message-processor instance
     */
    @Override
    protected void shutdownTakenMessagesExecutor() {
        shutdown(executor);
        executor = null;
    }

    /**
     * To get the logger of the executor implementation
     *
     * @return logger instance
     */
    @Override
    public Logger getLogger() {
        return log;
    }

    // inner classes
    // Command-Messages processor to process the request type messages
    @SuperBuilder
    private static class RequestsProcessor extends LocalQueueMessageProcessor {
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
         * To process the taken request type message.
         *
         * @param message the command-message to be processed
         */
        @Override
        public void onTakenMessage(CommandMessage<?> message) {
            // setting up processing context for the working thread
            ActionContext.install(message.getActionContext());
            try {
                log.debug("Starting request's processing with direction:{} correlation-id:{}",
                        message.getDirection(), message.getCorrelationId());
                // process taken message in the data transaction
                exchange.onTakenRequestMessage(message);
                log.debug(" ++ Successfully processed request with direction:{} correlation-id:{}",
                        message.getDirection(), message.getCorrelationId());
            } catch (Throwable e) {
                // process message after the error has thrown
                log.error("== Couldn't process message request with correlation-id:{}", message.getCorrelationId(), e);
                exchange.onErrorRequestMessage(message, e);
            } finally {
                // release current processing context
                ActionContext.release();
            }
        }
    }

    // Command-Messages processor to process the response type messages
    @SuperBuilder
    private static class ResponsesProcessor extends LocalQueueMessageProcessor {
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
         * To process the taken response type message.
         *
         * @param message the command-message to be processed
         */
        @Override
        public void onTakenMessage(CommandMessage<?> message) {
            exchange.onTakenResponseMessage(message);
        }
    }
}
