package oleg.sopilnyak.test.service.facade.impl.message;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.exception.CountDownLatchInterruptedException;
import oleg.sopilnyak.test.service.facade.ActionFacade;
import oleg.sopilnyak.test.service.message.CommandMessage;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;

/**
 * Service Implementation (Basic): execute command using request/response model (Local version through blocking queues)
 *
 * @see CommandThroughMessageService
 * @see ActionExecutor#processActionCommand(CommandMessage)
 */

//@Slf4j
//@RequiredArgsConstructor
public abstract class CommandThroughMessageServiceAdapter implements CommandThroughMessageService {
    //
    // @see ActionExecutor#processActionCommand(CommandMessage)
    private static final Logger log = LoggerFactory.getLogger("Low Level Command Action Executor");
    private static final ActionExecutor lowLevelActionExecutor = () -> log;
    // Executors for background processing
    private ExecutorService controlExecutorService;
    private ExecutorService messagesExecutorService;
    // Flag to control the current state of the service
    private final AtomicBoolean serviceActive = new AtomicBoolean(false);
    // The map of messages in progress, key is correlationId
    private final ConcurrentMap<String, MessageProgressWatchdog<?>> messageInProgress = new ConcurrentHashMap<>();
    // Declare income and outcome message processors
    private MessagesProcessor inputProcessor = null;
    private MessagesProcessor outputProcessor = null;

    /**
     * Start background processors for requests
     */
    @Override
    @PostConstruct
    public void initialize() {
        if (serviceActive.get()) {
            getLogger().warn("MessageProcessingService Local Version is already started.");
            return;
        }
        // adjust background processors
        // control executor for request/response processors
        controlExecutorService = createExecutorService("ProcessorControl-");
        // operational executor for processing command messages
        messagesExecutorService = createExecutorService("QueueMessageProcessor-");

        // starting background processors
        serviceActive.getAndSet(true);

        // launch command messages processors
        final CountDownLatch latchOfProcessors = new CountDownLatch(2);
        //
        // prepare and launch command requests processor
        launchInputProcessor(latchOfProcessors);
        //
        // prepare and launch command responses processor
        launchOutputProcessor(latchOfProcessors);
        //
        // waiting for processors' start
        waitFor(latchOfProcessors);
        //
        // the command through messages service is started
        getLogger().info("MessageProcessingService Local Version is started.");
    }

    /**
     * To get access to service's logger
     *
     * @return reference to concrete
     */
    protected abstract Logger getLogger();

    /**
     * Build and prepare message-processor for requests messages
     *
     * @param serviceActive the state of service
     * @param executor executor to launch taken message's processing
     * @return built and prepared messages-processor instance
     */
    protected abstract MessagesProcessor prepareInputProcessor(
            final AtomicBoolean serviceActive, final Executor executor
    );


    /**
     * Build and prepare message-processor for responses messages
     *
     * @param serviceActive the state of service
     * @param executor executor to launch taken message's processing
     * @return built and prepared messages-processor instance
     */
    protected abstract MessagesProcessor prepareOutputProcessor(
            final AtomicBoolean serviceActive, final Executor executor
    );

    /**
     * Stop background processors for requests
     */
    @Override
    @PreDestroy
    public void shutdown() {
        if (!serviceActive.get()) {
            getLogger().warn("MessageProcessingService Local Version is already stopped.");
            return;
        }
        // clear main flag of the service
        serviceActive.getAndSet(false);
        // send to processors final messages
        inputProcessor.accept(CommandMessage.EMPTY);
        outputProcessor.accept(CommandMessage.EMPTY);
        // wait for messages processors are active
        waitForProcessorsAreActive();
        // shutdown messages executors
        shutdown(messagesExecutorService);
        shutdown(controlExecutorService);
        // clear messages executors references
        controlExecutorService = null;
        messagesExecutorService = null;
        getLogger().info("MessageProcessingService Local Version is stopped.");
    }

    /**
     * To wait for any messages-processor is active
     */
    protected abstract void waitForProcessorsAreActive();

    /**
     * To find active message's watcher by correlatio-id
     *
     * @param correlationId the correlation-id of message to find
     * @return found message's watcher or empty
     * @see MessageProgressWatchdog
     * @see Optional#ofNullable(Object)
     */
    protected Optional<MessageProgressWatchdog<?>> findMessageInProgress(final String correlationId) {
        return Optional.ofNullable(messageInProgress.get(correlationId));
    }

    /**
     * To send command context message for processing
     *
     * @param message the command message to be sent
     * @param <T>     type of command execution result
     * @see MessagesProcessor#accept(CommandMessage)
     * @see CommandMessage
     */
    @Override
    public <T> void send(final CommandMessage<T> message) {
        final String messageCorrelationId = message.getCorrelationId();
        if (!inputProcessor.isProcessorActive()) {
            getLogger().warn(
                    "Send: '{}' is NOT active. Message with correlationId='{}' won't accept for processing.",
                    inputProcessor.getProcessorName(), messageCorrelationId
            );
            ActionFacade.throwFor(
                    message.getContext().getCommand().getId(),
                    new IllegalStateException(inputProcessor.getProcessorName() + " isn't in active state.")
            );
        }
        //
        // put message-watcher to in-progress map by correlation-id
        if (messageInProgress.putIfAbsent(messageCorrelationId, new MessageProgressWatchdog<>()) != null) {
            getLogger().warn("Send: message with correlationId='{}' is already in progress", messageCorrelationId);
        } else {
            // passing the message to the requests processor
            if (inputProcessor.accept(message)) {
                getLogger().info("Send: message with correlationId='{}' is accepted for processing.", messageCorrelationId);
            } else {
                getLogger().error("Send: message with correlationId='{}' is NOT accepted for processing", messageCorrelationId);
            }
        }
    }

    /**
     * To receive processed command message (sent by send(message)) by correlationId
     *
     * @param commandId            the command id of the command in the message
     * @param messageCorrelationId the correlation id to find processed command message
     * @return the processed command message
     * @see CommandMessage
     * @see MessageProgressWatchdog
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> CommandMessage<T> receive(String commandId, String messageCorrelationId) {
        if (outputProcessor.isProcessorActive()) {
            // getting message-watcher from in-progress map by correlation-id
            final MessageProgressWatchdog<T> messageWatcher = (MessageProgressWatchdog<T>) messageInProgress.get(messageCorrelationId);
            if (isNull(messageWatcher)) {
                logMessageIsNotInProgress(messageCorrelationId);
                return null;
            }
            getLogger().info("Receive: waiting for sent message of command id: '{}' complete in message {}", commandId, messageCorrelationId);
            // wait until processing is completed
            messageWatcher.waitForMessageComplete();
            // remove message-watcher from in-progress map by correlation-id
            messageInProgress.remove(messageCorrelationId);
            // return the result
            final CommandMessage<T> result = messageWatcher.getResult();
            getLogger().info("Receive: the result of command '{}' is {}", commandId, result);
            return result;
        } else {
            getLogger().warn("{} is NOT active. Message with correlationId='{}' is won't receive",
                    outputProcessor.getProcessorName(), messageCorrelationId);
            return ActionFacade.throwFor(commandId, new IllegalStateException(outputProcessor.getProcessorName() + " is NOT active."));
        }
    }

    /**
     * To process the request message's processing command in new transaction (strong isolation)<BR/>
     * and send the response to the responses queue
     *
     * @param message message to process
     * @see ActionExecutor#processActionCommand(CommandMessage)
     */
    protected <T> void processTakenRequestCommandMessage(final CommandMessage<T> message) {
        final String correlationId = message.getCorrelationId();
        getLogger().debug("Processing request message with correlationId='{}'", correlationId);
        // check if the request in messages in progress map by correlation-id
        Optional.ofNullable(messageInProgress.get(correlationId)).ifPresentOrElse(_ -> {
                    //
                    // process the request's command locally and send the result to the responses queue
                    final CommandMessage<T> result = lowLevelActionExecutor.processActionCommand(message);
                    getLogger().debug("Processed request message with correlationId='{}'", correlationId);
                    //
                    // finalize message's processing
                    finalizeProcessedMessage(result, correlationId);
                },
                () -> logMessageIsNotInProgress(correlationId)
        );
    }

    /**
     * Processing an error after taken message-command wrong processing
     *
     * @param message request command-message
     * @param error   cause of message processing error
     */
    protected void processUnprocessedRequestCommandMessage(final CommandMessage<?> message, final Throwable error) {
        if (!message.getContext().isFailed()) {
            getLogger().error("=+= Context not failed but something thrown after {}", message.getContext(), error);
            if (error instanceof Exception exception) {
                message.getContext().failed(exception);
            } else {
                getLogger().warn("=?= Something strange was thrown =?=", error);
                return;
            }
        }
        final String correlationId = message.getCorrelationId();
        getLogger().error("== Sending failed context of message {}", message.getCorrelationId());
        // finalize message's processing
        finalizeProcessedMessage(message, correlationId);
    }

    // private methods
    // create and configure execution service
    private static ExecutorService createExecutorService(final String threadNamePrefix) {
        final var threadsFactory = new CustomizableThreadFactory(threadNamePrefix);
        threadsFactory.setThreadGroupName("Command-Through-Message-Threads");
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    // shut down execution service properly
    private static void shutdown(final ExecutorService executor) {
        executor.shutdown(); // Stop accepting new tasks
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // Force stop if not finished
            }
        } catch (InterruptedException _) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            executor.close();
        }
    }
    // log that message with correlationId is not found in progress map
    private void logMessageIsNotInProgress(final String correlationId) {
        getLogger().warn("= Message with correlationId='{}' is NOT found in progress map", correlationId);
    }

    // launching command context requests processor
    private void launchInputProcessor(CountDownLatch latchOfProcessors) {
        // preparing launch command context requests processor
        inputProcessor = prepareInputProcessor(serviceActive, messagesExecutorService);
        launchMessagesProcessor(inputProcessor, latchOfProcessors, controlExecutorService);
    }

    // launching command context responses processor
    private void launchOutputProcessor(CountDownLatch latchOfProcessors) {
        // preparing launch command context responses processor
        outputProcessor = prepareOutputProcessor(serviceActive, messagesExecutorService);
        launchMessagesProcessor(outputProcessor, latchOfProcessors, controlExecutorService);
    }

    // launching command messages processor
    private static void launchMessagesProcessor(MessagesProcessor messagesProcessor,
                                                CountDownLatch latchOfProcessors,
                                                Executor processorLaunchExecutor) {
        final Runnable processorRunnable = () -> {
            // responses processor is going to start
            latchOfProcessors.countDown();
            // running responses processor
            messagesProcessor.processing();
        };
        // launching messages processor runnable
        CompletableFuture.runAsync(processorRunnable, processorLaunchExecutor);
    }

    // waiting for all processors' became started
    private static void waitFor(final CountDownLatch processorsStarting) {
        try {
            processorsStarting.await();
        } catch (InterruptedException e) {
            /* Clean up whatever needs to be handled before interrupting  */
            Thread.currentThread().interrupt();
            throw new CountDownLatchInterruptedException(2, e);
        }
    }

    // finalize processed message
    private void finalizeProcessedMessage(final CommandMessage<?> processedMessage, final String correlationId) {
        // try to send result to the responses queue
        if (outputProcessor.accept(processedMessage)) {
            // successfully sent
            getLogger().debug("Message with correlationId='{}' is processed and put to responses processor", correlationId);
        } else {
            // something went wrong
            getLogger().error("Message with correlationId='{}' is processed but is NOT sent to responses processor", correlationId);
        }
    }
}
