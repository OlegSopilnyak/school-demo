package oleg.sopilnyak.test.service.command.executable.core.executor.messaging.local;


import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.CommandMessageWatchdog;
import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.CommandMessagesExchangeExecutorAdapter;
import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.MessagesProcessor;
import oleg.sopilnyak.test.service.message.CommandMessage;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
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
        return messageInProgress.putIfAbsent(correlationId, new CommandMessageInProgressWatchdog<>(original)) == null;
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

    // inner class
    // Watcher: local command-message in progress watcher
    private static class CommandMessageInProgressWatchdog<T> implements CommandMessageWatchdog<T> {
        private final Duration duration;
        // original instance of the message to watch after
        private final CommandMessage<T> original;
        private final AtomicReference<CommandMessage<T>> result = new AtomicReference<>(null);
        @Getter
        private volatile State state = State.IN_PROGRESS;
        private final Object getResultMonitor = new Object();

        public CommandMessageInProgressWatchdog(CommandMessage<T> original) {
            this(original, Duration.ofMillis(1000L));
        }

        public CommandMessageInProgressWatchdog(CommandMessage<T> original, Duration duration) {
            this.original = original;
            this.duration = duration;
        }

        @Override
        public CommandMessage<T> getResult() {
            return result.get();
        }

        @Override
        public void setResult(CommandMessage<T> result) {
            if (result != null) {
                this.result.getAndSet(result);
                this.state = State.COMPLETED;
            }
        }

        @Override
        public void waitForMessageComplete() {
            synchronized (getResultMonitor) {
                result.getAndSet(null);
                final LocalDateTime startsAt = LocalDateTime.now();
                // waiting while state is in progress
                while (state == State.IN_PROGRESS) {
                    try {
                        getResultMonitor.wait(25);
                        // check result message expiration
                        if (Duration.between(startsAt, LocalDateTime.now()).compareTo(duration) > 0) {
                            // updating watchdog's state
                            state = State.EXPIRED;
                            result.getAndSet(original);
                            // updating result message context
                            final String errorMessage = "Expired message with id:" + getResult().getCorrelationId();
                            log.warn(errorMessage);
                            getResult().getContext().failed(new TimeoutException(errorMessage));
                            break;
                        }
                    } catch (InterruptedException e) {
                        log.warn("Interrupted while waiting for state to complete.", e);
                        /* Clean up whatever needs to be handled before interrupting  */
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        @Override
        public void messageProcessingIsDone() {
            synchronized (getResultMonitor) {
                if (state == State.COMPLETED) {
                    getResultMonitor.notifyAll();
                }
            }
        }
    }
}
