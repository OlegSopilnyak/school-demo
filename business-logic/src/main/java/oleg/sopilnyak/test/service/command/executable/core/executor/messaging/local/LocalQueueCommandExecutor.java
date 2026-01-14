package oleg.sopilnyak.test.service.command.executable.core.executor.messaging.local;


import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.CommandMessageWatchdog;
import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.CommandMessagesExchangeExecutorAdapter;
import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.MessagesProcessor;
import oleg.sopilnyak.test.service.message.CommandMessage;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    // The map of messages in progress, key is correlationId
    private final ConcurrentMap<String, CommandMessageWatchdog<?>> messageInProgress = new ConcurrentHashMap<>();
    // taken command-messages async processing executor
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
     * To run processor's taken message processing in asynchronous way
     * Runs in separate thread
     *
     * @param commandMessageProcessing taken message processing runner
     * @see MessagesProcessor#doingMainLoop()
     * @see CompletableFuture#runAsync(Runnable, Executor)
     */
    @Override
    public void runAsync(Runnable commandMessageProcessing) {
        CompletableFuture.runAsync(commandMessageProcessing, executor);
    }

    /**
     * To prepare and start message watcher for the command-message
     *
     * @param correlationId correlation-id of message to watch after
     * @param original      original message to watch after
     * @return true if it's made
     */
    @Override
    protected boolean makeMessageInProgress(String correlationId, CommandMessage<?> original) {
        return messageInProgress.putIfAbsent(correlationId, new LocalMessageInProgressWatchdog<>(original)) == null;
    }

    /**
     * To get the watcher of in-progress message
     *
     * @param correlationId correlation-id of watching message
     * @return command-message watcher
     */
    @Override
    @SuppressWarnings("unchecked")
    protected <T> Optional<CommandMessageWatchdog<T>> messageWatchdogFor(String correlationId) {
        return Optional.ofNullable((CommandMessageWatchdog<T>) messageInProgress.get(correlationId));
    }

    /**
     * To stop watching after of the command-message
     *
     * @param correlationId correlation-id of command-message to stop watching after
     */
    @Override
    protected void stopWatchingMessage(String correlationId) {
        messageInProgress.remove(correlationId);
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
        return LocalQueueMessageProcessor.builder()
                .processorName(REQUEST_MESSAGES_PROCESSOR_NAME)
                .logger(log).exchange(this).objectMapper(objectMapper)
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
        return LocalQueueMessageProcessor.builder()
                .processorName(RESPONSE_MESSAGES_PROCESSOR_NAME)
                .logger(log).exchange(this).objectMapper(objectMapper)
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
}
