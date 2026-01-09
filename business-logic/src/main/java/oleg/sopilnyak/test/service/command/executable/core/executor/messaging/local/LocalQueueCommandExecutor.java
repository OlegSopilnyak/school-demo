package oleg.sopilnyak.test.service.command.executable.core.executor.messaging.local;


import static oleg.sopilnyak.test.service.message.CommandThroughMessageService.COMMAND_MESSAGE_OBJECT_MAPPER_BEAN_NAME;

import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.CommandMessagesExchangeExecutorAdapter;
import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.MessagesProcessor;
import oleg.sopilnyak.test.service.message.CommandMessage;

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
    public static final String REQUEST_MESSAGES_PROCESSOR_NAME = "RequestMessagesProcessor";
    public static final String RESPONSE_MESSAGES_PROCESSOR_NAME = "ResponseMessagesProcessor";
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
     * @see oleg.sopilnyak.test.service.command.executable.core.executor.messaging.RootMessageProcessor
     * @see CommandMessagesExchangeExecutorAdapter#initialize()
     * @see CommandMessagesExchangeExecutorAdapter#prepareRequestsProcessor()
     * @see CommandMessagesExchangeExecutorAdapter#onTakenRequestMessage(CommandMessage)
     * @see CommandMessagesExchangeExecutorAdapter#onErrorRequestMessage(CommandMessage, Throwable)
     */
    @Override
    protected MessagesProcessor prepareRequestsProcessor() {
        return LocalMessageProcessor.builder()
                .processorName(REQUEST_MESSAGES_PROCESSOR_NAME)
                .executor(executor).logger(log).exchange(this).objectMapper(objectMapper)
                .processingTaken(this::executeWithActionContext)
                .build();
    }

    /**
     * Build and prepare message-processor for responses messages
     *
     * @return built and prepared messages-processor instance
     * @see CommandMessagesExchangeExecutorAdapter#initialize()
     * @see CommandMessagesExchangeExecutorAdapter#prepareResponsesProcessor()
     * @see CommandMessagesExchangeExecutorAdapter#onTakenResponseMessage(CommandMessage)
     */
    @Override
    protected MessagesProcessor prepareResponsesProcessor() {
        return LocalMessageProcessor.builder()
                .processorName(RESPONSE_MESSAGES_PROCESSOR_NAME)
                .executor(executor).logger(log).exchange(this).objectMapper(objectMapper)
                .processingTaken(this::onTakenResponseMessage).build();
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
    // Command-Messages processor to process all message types
    @SuperBuilder
    private static class LocalMessageProcessor extends LocalQueueMessageProcessor {
    }
}
