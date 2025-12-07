package oleg.sopilnyak.test.service.facade.impl;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.exception.CountDownLatchInterruptedException;
import oleg.sopilnyak.test.service.facade.ActionFacade;
import oleg.sopilnyak.test.service.facade.impl.message.MessageProgressWatchdog;
import oleg.sopilnyak.test.service.facade.impl.message.MessagesProcessorAdapter;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandMessage;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service Implementation: execute command using request/response model (Local version through blocking queues)
 *
 * @see CommandThroughMessageService
 * @see ActionExecutor#processActionCommand(CommandMessage)
 * @see BlockingQueue
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class CommandThroughMessageServiceLocalImpl implements CommandThroughMessageService {
    // Flags to control the current state of the processors
    private static final Map<Class<? extends MessagesProcessorAdapter>, AtomicBoolean> processorStates = Map.of(
            RequestsProcessor.class, new AtomicBoolean(false),
            ResponsesProcessor.class, new AtomicBoolean(false)
    );
    //
    // maximum size of threads pool for Messages Queue Processor
    @Value("${school.maximum.threads.pool.size:10}")
    private int maximumPoolSize;
    // Create executor for processing message commands by default
    // @see ActionExecutor#processActionCommand(CommandMessage)
    private static final ActionExecutor basicActionExecutor = () -> log;
    // Executor services for background processing
    private ExecutorService controlExecutorService;
    private ExecutorService operationalExecutorService;
    // Flag to control the current state of the service
    private final AtomicBoolean serviceActive = new AtomicBoolean(false);
    // The map of messages in progress, key is correlationId
    private final ConcurrentMap<String, MessageProgressWatchdog<?>> messageInProgress = new ConcurrentHashMap<>();
    // Declare income and outcome message processors
    private MessagesProcessorAdapter inputProcessor = null;
    private MessagesProcessorAdapter outputProcessor = null;

    /**
     * Start background processors for requests
     */
    @Override
    @PostConstruct
    public void initialize() {
        if (serviceActive.get()) {
            log.warn("MessageProcessingService Local Version is already started.");
            return;
        }
        // adjust background processors
        // control executor for request/response processors
        controlExecutorService = createExecutorService(2, "ProcessorControl-");
        // operational executor for processing command messages
        final int operationalPoolSize = Math.max(maximumPoolSize, Runtime.getRuntime().availableProcessors());
        operationalExecutorService = createExecutorService(operationalPoolSize, "QueueMessageProcessor-");

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
        log.info("MessageProcessingService Local Version is started.");
    }

    protected MessagesProcessorAdapter prepareInputProcessor(
            AtomicBoolean serviceActive,
            ExecutorService executorService,
            Logger log) {
        final AtomicBoolean requestsProcessorState = processorStates.get(RequestsProcessor.class);
        return new RequestsProcessor(requestsProcessorState, serviceActive, executorService, log);
    }

    protected MessagesProcessorAdapter prepareOutputProcessor(
            AtomicBoolean serviceActive,
            ExecutorService executorService,
            Logger log) {
        final AtomicBoolean responsesProcessorState = processorStates.get(ResponsesProcessor.class);
        return new ResponsesProcessor(responsesProcessorState, serviceActive, executorService, log);
    }

    /**
     * Stop background processors for requests
     */
    @Override
    @PreDestroy
    public void shutdown() {
        if (!serviceActive.get()) {
            log.warn("MessageProcessingService Local Version is already stopped.");
            return;
        }
        // clear main flag of the service
        serviceActive.getAndSet(false);
        // send to processors final messages
        inputProcessor.apply(BaseCommandMessage.EMPTY);
        outputProcessor.apply(BaseCommandMessage.EMPTY);
        // wait for messages processors are active
        waitForProcessorsAreActive();
        // shutdown messages executors
        shutdown(operationalExecutorService);
        shutdown(controlExecutorService);
        // clear messages executors references
        controlExecutorService = null;
        operationalExecutorService = null;
        log.info("MessageProcessingService Local Version is stopped.");
    }

    /**
     * To send command context message for processing
     *
     * @param message the command message to be sent
     * @param <T>     type of command execution result
     * @see MessagesProcessorAdapter#apply(CommandMessage)
     * @see CommandMessage
     */
    @Override
    public <T> void send(final CommandMessage<T> message) {
        final String messageCorrelationId = message.getCorrelationId();
        if (!inputProcessor.isProcessorActive()) {
            log.warn(
                    "RequestMessagesProcessor is NOT active. Message with correlationId='{}' won't sent for processing.",
                    messageCorrelationId
            );
            ActionFacade.throwFor(
                    message.getContext().getCommand().getId(),
                    new IllegalStateException("RequestMessagesProcessor isn't in active state.")
            );
        }
        //
        // put message-watcher to in-progress map by correlation-id
        if (messageInProgress.putIfAbsent(messageCorrelationId, new MessageProgressWatchdog<>()) != null) {
            log.warn("Message with correlationId='{}' is already in progress", messageCorrelationId);
        } else {
            // passing the message to the requests processor
            if (inputProcessor.apply(message)) {
                log.debug("Message with correlationId='{}' is accepted for processing", messageCorrelationId);
            } else {
                log.error("Message with correlationId='{}' is NOT accepted for processing", messageCorrelationId);
            }
        }
    }

    /**
     * To receive processed command message (sent by send(message)) by correlationId
     *
     * @param commandId            the command id of the command in the message
     * @param messageCorrelationId the correlation id to find processed command message
     * @return the processed command message
     * @see RequestsProcessor#processActionCommandAndProceed(CommandMessage)
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
            // wait until processing is completed
            messageWatcher.waitForMessageComplete();
            // remove message-watcher from in-progress map by correlation-id
            messageInProgress.remove(messageCorrelationId);
            // return the result
            final CommandMessage<T> result = messageWatcher.getResult();
            log.info("The result of command '{}' is {}", commandId, result);
            return result;
        } else {
            log.warn("ResponseMessagesProcessor is NOT active. Message with correlationId='{}' is NOT received", messageCorrelationId);
            return ActionFacade.throwFor(commandId, new IllegalStateException("ResponseMessagesProcessor is NOT active."));
        }
    }

    // private methods
    // log that message with correlationId is not found in progress map
    private static void logMessageIsNotInProgress(final String correlationId) {
        log.warn("= Message with correlationId='{}' is NOT found in progress map", correlationId);
    }

    // wait for messages processors are active
    private void waitForProcessorsAreActive() {
        final Object monitor = new Object();
        while (processorStates.values().stream().anyMatch(AtomicBoolean::get)) synchronized (monitor) {
            try {
                monitor.wait(25);
            } catch (InterruptedException _) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // create and configure execution service
    private static ExecutorService createExecutorService(final int maxPoolSize, final String threadNamePrefix) {
        final var threadsFactory = new CustomizableThreadFactory(threadNamePrefix);
        threadsFactory.setThreadGroupName("Command-Through-Message-Threads");
        return Executors.newScheduledThreadPool(maxPoolSize, threadsFactory);
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

    // launching command context requests processor
    private void launchInputProcessor(CountDownLatch latchOfProcessors) {
        // preparing launch command context requests processor
        inputProcessor = prepareInputProcessor(serviceActive, operationalExecutorService, log);
        launchMessagesProcessor(inputProcessor, latchOfProcessors, controlExecutorService);
    }

    // launching command context responses processor
    private void launchOutputProcessor(CountDownLatch latchOfProcessors) {
        // preparing launch command context responses processor
        outputProcessor = prepareOutputProcessor(serviceActive, operationalExecutorService, log);
        launchMessagesProcessor(outputProcessor, latchOfProcessors, controlExecutorService);
    }

    // launching command messages processor
    private static void launchMessagesProcessor(MessagesProcessorAdapter messagesProcessor,
                                                CountDownLatch latchOfProcessors,
                                                ExecutorService processorRunExecutorService) {
        final Runnable processorRunnable = () -> {
            // responses processor is going to start
            latchOfProcessors.countDown();
            // running responses processor
            messagesProcessor.processing();
        };
        // launching messages processor runnable
        CompletableFuture.runAsync(processorRunnable, processorRunExecutorService);
    }

    // waiting for processors' start
    private static void waitFor(final CountDownLatch processorsStarting) {
        try {
            processorsStarting.await();
        } catch (InterruptedException e) {
            /* Clean up whatever needs to be handled before interrupting  */
            Thread.currentThread().interrupt();
            throw new CountDownLatchInterruptedException(2, e);
        }
    }

    // inner classes
    // Parent class of messages processing processor, using local blocking-queue
    private static class MessagesProcessorLocal extends MessagesProcessorAdapter {
        private final BlockingQueue<CommandMessage<?>> messages = new LinkedBlockingQueue<>();

        protected MessagesProcessorLocal(AtomicBoolean processorActive,
                                         AtomicBoolean serviceActive,
                                         Executor executor, Logger log) {
            super(processorActive, serviceActive, executor, log);
        }

        /**
         * To apply for processing command-message
         *
         * @param message command-message to process
         */
        @Override
        public <T> boolean apply(CommandMessage<T> message) {
            log.trace("Put to queue command message {}", message);
            return messages.add(message);
        }

        /**
         * To check are there are active messages to process
         *
         * @return true if processor is waiting for the message
         */
        @Override
        public boolean isEmpty() {
            return messages.isEmpty();
        }

        /**
         * Get the name of the processor for logging purposes.
         *
         * @return the name of the processor
         */
        @Override
        public String getProcessorName() {
            throw new UnsupportedOperationException("Please implement me!");
        }

        /**
         * To take command message from the appropriate queue for further processing
         *
         * @return the command message taken from the appropriate queue
         * @throws InterruptedException if interrupted while waiting
         * @see BaseCommandMessage
         */
        @Override
        protected <T> CommandMessage<T> takeFromQueue() throws InterruptedException {
            return (CommandMessage<T>) messages.take();
        }

        /**
         * To process the taken message.
         *
         * @param message the command message to be processed
         */
        @Override
        protected void processTakenMessage(CommandMessage<?> message) {
            throw new UnsupportedOperationException("Please implement me!");
        }
    }

    // Messages processor for background processing of requests
    private class RequestsProcessor extends MessagesProcessorLocal {
        private RequestsProcessor(AtomicBoolean processor, AtomicBoolean active, Executor executor, Logger logger) {
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
        protected void processTakenMessage(final CommandMessage<?> message) {
            // setting up processing context for current thread
            ActionContext.install(message.getActionContext());
            try {
                log.debug("Start processing request with direction:{} correlation-id:{}",
                        message.getDirection(), message.getCorrelationId());
                // process message in the transaction
                processActionCommandAndProceed(message);
                log.debug(" ++ Successfully processed request with direction:{} correlation-id:{}",
                        message.getDirection(), message.getCorrelationId());
            } catch (Throwable e) {
                log.error("== Cannot process message {}", message.getCorrelationId(), e);
                processWrongProcessingMessage(message, e);
            } finally {
                // release current processing context
                ActionContext.release();
            }
        }

        /**
         * Processing an error after message-command wrong processing
         *
         * @param message request command-message
         * @param error cause of message processing error
         */
        void processWrongProcessingMessage(CommandMessage<?> message, Throwable error) {
            if (!message.getContext().isFailed()) {
                log.error("=+= Context not failed but something thrown after {}", message.getContext(), error);
                if (error instanceof Exception exception) {
                    message.getContext().failed(exception);
                } else {
                    log.warn("=?= Something strange was thrown =?=", error);
                }
            }
            log.error("== Sending failed context of message {}", message.getCorrelationId());
            outputProcessor.apply(message);
        }

        /**
         * To process the request message's processing command in new transaction (strong isolation)<BR/>
         * and send the response to the responses queue
         *
         * @param requestMessage message to process
         * @see ActionExecutor#processActionCommand(CommandMessage)
         */
        <T> void processActionCommandAndProceed(final CommandMessage<T> requestMessage) {
            final String correlationId = requestMessage.getCorrelationId();
            log.debug("Processing request message with correlationId='{}'", correlationId);
            // check if the request in progress map by correlation-id
            if (!messageInProgress.containsKey(correlationId)) {
                logMessageIsNotInProgress(correlationId);
                return;
            }
            // process the request's command locally and send the result to the responses queue
            final CommandMessage<T> processedMessage = basicActionExecutor.processActionCommand(requestMessage);
            log.debug("Processed request message with correlationId='{}'", correlationId);
            //
            // try to send result to the responses queue
            if (outputProcessor.apply(processedMessage)) {
                // successfully sent
                log.debug("Message with correlationId='{}' is processed and sent to responses processor", correlationId);
            } else {
                // something went wrong
                log.error("Message with correlationId='{}' is processed but NOT added to responses queue", correlationId);
            }
        }
    }

    // Messages processor for background processing of responses
    private class ResponsesProcessor extends MessagesProcessorLocal {
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

        @Override
        protected void processTakenMessage(CommandMessage<?> message) {
            log.info("Start processing response with correlationId='{}' which is needs completion", message.getCorrelationId());
            completeMessageProcessing(message);
        }

        @SuppressWarnings("unchecked")
        // complete the message processing
        private void completeMessageProcessing(final CommandMessage response) {
            final String correlationId = response.getCorrelationId();
            // check if the message in progress map by correlation-id
            final MessageProgressWatchdog<?> messageWatcher;
            if (isNull(messageWatcher = messageInProgress.get(correlationId))) {
                logMessageIsNotInProgress(correlationId);
                return;
            }
            // set up the result and mark as completed
            messageWatcher.setResult(response);
            // notify thread waiting for this message-watcher result
            messageWatcher.messageProcessingIsDone();
            log.info("Successfully processed response with correlationId='{}'", correlationId);
        }
    }
}
