package oleg.sopilnyak.test.service.command.executable.core.executor.messaging;

import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.exception.CountDownLatchInterruptedException;
import oleg.sopilnyak.test.service.facade.ActionFacade;
import oleg.sopilnyak.test.service.message.CommandMessage;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import lombok.Getter;

/**
 * Facade (Base Implementation): The main engine to execute school command activities through messages exchange.<BR/>
 * Implements business-logic based on command-messages exchange
 *
 * @see CommandActionExecutor
 */
public abstract class CommandMessagesExchangeExecutorAdapter extends MessagesExchange implements CommandActionExecutor {
    // executor service to support message-processors lifecycle
    private ExecutorService processorLaunchExecutor = null;
    // Flag to control the current state of the service
    private final AtomicBoolean serviceActive = new AtomicBoolean(false);
    // Declare income and outcome message processors
    private MessagesProcessor requestsProcessor = null;
    @Getter
    private MessagesProcessor responsesProcessor = null;

    /**
     * To check the state of messages exchange sub-service
     *
     * @return true if it's active
     */
    @Override
    public boolean isActive() {
        return serviceActive.get();
    }

    /**
     * To initialize school-commands executor
     */
    @Override
    @PostConstruct
    public void initialize() {
        if (isActive()) {
            getLogger().warn("The executor is already started.");
            return;
        }
        //
        // initializing command action executor
        getLogger().info("Initializing school-commands executor...");
        serviceActive.getAndSet(true);
        // control executor for request/response processors
        processorLaunchExecutor = buildExecutorService();
        // initialize taken messages executor service
        initializeTakenMessagesExecutor();
        // launch command messages processors
        final CountDownLatch latchOfProcessors = new CountDownLatch(2);
        //
        // prepare and launch command requests processor
        launchInProcessor(latchOfProcessors);
        //
        // prepare and launch command responses processor
        launchOutProcessor(latchOfProcessors);
        //
        // waiting for processors' start
        waitFor(latchOfProcessors);
        //
        // the command through messages service is started
        getLogger().info("The executor is started.");
    }

    /**
     * To shut down school-commands executor
     */
    @Override
    @PreDestroy
    public void shutdown() {
        if (!isActive()) {
            getLogger().warn("The executor is already stopped.");
            return;
        }
        //
        // shutting down command action executor
        getLogger().info("Shutting down school-commands executor...");
        serviceActive.getAndSet(false);
        // shutting down all messages processors
        shutdownMessageProcessors();
        // shutdown message-processor launcher executor
        shutdownProcessorsLauncherExecutor();
        //
        // the command through messages service is stopped
        getLogger().info("The executor is stopped.");
    }

    /**
     * To process command message, using messages-processors
     *
     * @param message the command message to process
     * @return processed command message
     * @see CommandMessage
     * @see MessagesProcessor
     */
    @Override
    public <T> CommandMessage<T> processActionCommand(final CommandMessage<T> message) {
        getLogger().debug("Validating input command-message before processing...");
        validateInput(message);
        // setup variables for command-message processing
        final String correlationId = message.getCorrelationId();
        final String commandId = message.getContext().getCommand().getId();
        //
        // start process sending command-message to requests messages processor
        getLogger().info("=== Sending command-message to start processing it, correlationId='{}'", correlationId);
        if (launchCommandMessageProcessing(message, correlationId, commandId)) {
            // waiting for processed command-message from responses processor
            getLogger().info("=== Waiting for processed command message of command '{}' with correlationId='{}'", commandId, correlationId);
            return waitingProcessedCommandMessage(correlationId, commandId);
        } else {
            getLogger().warn("Launching command:'{}' message:'{}' processing is canceled.", commandId, correlationId);
            // nothing to return
            return null;
        }
    }


    /**
     * To launch incoming command message processor
     *
     * @param latchOfProcessors latch of launched message processors
     */
    protected void launchInProcessor(final CountDownLatch latchOfProcessors) {
        // preparing launch incoming(requests) command messages processor
        requestsProcessor = prepareRequestsProcessor();
        launchProcessor(requestsProcessor, latchOfProcessors);
    }

    /**
     * Build and prepare message-processor for requests messages
     *
     * @return built and prepared messages-processor instance
     */
    protected abstract MessagesProcessor prepareRequestsProcessor();

    /**
     * To launch processed command message processor
     *
     * @param latchOfProcessors latch of launched message processors
     */
    protected void launchOutProcessor(CountDownLatch latchOfProcessors) {
        // preparing launch of processed(responses) command messages processor
        responsesProcessor = prepareResponsesProcessor();
        launchProcessor(responsesProcessor, latchOfProcessors);
    }

    /**
     * Build and prepare message-processor for responses messages
     *
     * @return built and prepared messages-processor instance
     */
    protected abstract MessagesProcessor prepareResponsesProcessor();

    /**
     * To initialize executor service of the taken messages
     */
    protected abstract void initializeTakenMessagesExecutor();

    /**
     * To build threads factory for service executors threads
     *
     * @param threadNamePrefix the prefix of create thread
     * @return built threads factory
     */
    protected static ThreadFactory serviceThreadFactory(final String threadNamePrefix) {
        final var threadsFactory = new CustomizableThreadFactory(threadNamePrefix);
        threadsFactory.setThreadGroupName("Command-Through-Message-Threads");
        return threadsFactory;
    }

    /**
     * To shut down executor service for messages taken by message-processor instance
     */
    protected abstract void shutdownTakenMessagesExecutor();

    /**
     * To shut down tasks executor service properly
     *
     * @param executor executor service to shut down
     */
    protected static void shutdown(final ExecutorService executor) {
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

    // private methods
    // shutting down processors and services around them
    private void shutdownMessageProcessors() {
        requestsProcessor.shutdown();
        responsesProcessor.shutdown();
        // clearing messages processors references
        requestsProcessor = null;
        responsesProcessor = null;
        // shutting down taken command-messages processing executor service
        shutdownTakenMessagesExecutor();
    }

    // shutting down message-processors launcher executor service
    private void shutdownProcessorsLauncherExecutor() {
        shutdown(processorLaunchExecutor);
        // clearing messages executor reference
        processorLaunchExecutor = null;
    }

    // to launch command-message processing, sending command-message to requests messages processor
    private <T> boolean launchCommandMessageProcessing(final CommandMessage<T> message, final String correlationId, final String commandId) {
        if (!requestsProcessor.isProcessorActive()) {
            getLogger().warn(
                    "Launch: '{}' is NOT active. Message with correlationId='{}' won't accept for processing.",
                    requestsProcessor.getProcessorName(), correlationId
            );
            final String errorMessage = requestsProcessor.getProcessorName() + " isn't in active state.";
            ActionFacade.throwFor(commandId, new IllegalStateException(errorMessage));
        }
        //
        // put message to in-progress map by correlation-id (make it in-progress)
        if (makeMessageInProgress(correlationId, message)) {
            // passing the message to the requests processor
            return initiateProcessingMessage(message);
        } else {
            // message with correlation-id already in progress
            getLogger().warn("Launch: message with correlationId='{}' is already in progress", correlationId);
            return false;
        }
    }

    // initialize request command-message processing
    private boolean initiateProcessingMessage(final CommandMessage<?> message) {
        // try to send the request to the requests processor
        if (requestsProcessor.accept(message)) {
            // successfully sent
            getLogger().info("Launch: message with correlationId='{}' is accepted for processing.", message.getCorrelationId());
            // initiated well
            return true;
        }
        // something went wrong
        final String messageCorrelationId = message.getCorrelationId();
        getLogger().error("Launch: message with correlationId='{}' is NOT accepted for processing.", messageCorrelationId);
        // removing message-watcher from in-progress-messages map using correlation-id
        stopWatchingMessage(messageCorrelationId);
        getLogger().error("Launch: removed message-watcher for correlationId='{}' from in-progress-messages map.", messageCorrelationId);
        // could not initiate
        return false;
    }

    // To receive processed command message (sent by initiateProcessingMessage(...)) by correlationId
    private <T> @Nullable CommandMessage<T> waitingProcessedCommandMessage(String correlationId, String commandId) {
        if (responsesProcessor.isProcessorActive()) {
            return retrieveProcessedMessage(commandId, correlationId);
        } else {
            final String processorName = responsesProcessor.getProcessorName();
            getLogger().warn("{} is NOT active. Message with correlationId='{}' is won't receive", processorName, correlationId);
            return ActionFacade.throwFor(commandId, new IllegalStateException(processorName + " is NOT active."));
        }
    }

    // waiting for processed command-message from responses processor
    private <T> @Nullable CommandMessage<T> retrieveProcessedMessage(final String commandId, final String correlationId) {
        // command-message in progress holder
        final AtomicReference<CommandMessage<T>> processedMessageHolder = new AtomicReference<>(null);
        // getting message-watcher from in-progress map by correlation-id
        final Optional<MessageProgressWatchdog<T>> watchdogOptional = messageWatchdogFor(correlationId);
        // waiting for and get processed command result
        watchdogOptional.ifPresentOrElse(watchdog -> {
                    getLogger().info(
                            "= Retrieve: waiting for sent command: '{}' process completion, in the message: '{}'",
                            commandId, correlationId
                    );
                    // wait until command-message processing is done
                    watchdog.waitForMessageComplete();
                    // removing message-watcher from message-in-progress map using correlation-id
                    stopWatchingMessage(correlationId);
                    // getting processed result
                    final CommandMessage<T> processedCommandResult = watchdog.getResult();
                    getLogger().debug(
                            "= Retrieve: the result of command '{}' after processing is {}",
                            commandId, processedCommandResult
                    );
                    processedMessageHolder.getAndSet(processedCommandResult);
                },
                // no command-message-watcher in message-in-progress map
                () -> getLogger().warn(
                        "= Retrieve: the message with correlationId='{}' is NOT found in message-in-progress map",
                        correlationId
                )
        );
        // returns processed command-message value
        return processedMessageHolder.get();
    }

    // create and configure messages processor execution service
    private static ExecutorService buildExecutorService() {
        return Executors.newFixedThreadPool(2, serviceThreadFactory("MessageProcessorThread-"));
    }

    // launching command messages processor asynchronously
    private void launchProcessor(final MessagesProcessor processor, final CountDownLatch latch) {
        // launching messages processor runnable
        CompletableFuture.runAsync(() -> {
                    // command-messages processor is going to start
                    latch.countDown();
                    // running command-messages processor main loop
                    processor.doingMainLoop();
                },
                processorLaunchExecutor
        );
    }

    // waiting for all processors' became started
    private static void waitFor(final CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            /* Clean up whatever needs to be handled before interrupting  */
            Thread.currentThread().interrupt();
            throw new CountDownLatchInterruptedException(2, e);
        }
    }
}
