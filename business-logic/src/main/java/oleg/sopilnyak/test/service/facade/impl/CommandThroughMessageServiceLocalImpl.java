package oleg.sopilnyak.test.service.facade.impl;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.exception.CountDownLatchInterruptedException;
import oleg.sopilnyak.test.service.facade.ActionFacade;
import oleg.sopilnyak.test.service.facade.impl.message.MessageProgressWatchdog;
import oleg.sopilnyak.test.service.facade.impl.message.MessagesProcessorAdapter;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
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
 * @see ActionExecutor#processActionCommand(BaseCommandMessage)
 * @see BlockingQueue
 */

@Slf4j
@RequiredArgsConstructor
@Service
public class CommandThroughMessageServiceLocalImpl implements CommandThroughMessageService {
    //
    // maximum size of threads pool for Messages Queue Processor
    @Value("${school.maximum.threads.pool.size:10}")
    private int maximumPoolSize;
    // Create executor for processing message commands by default
    // @see ActionExecutor#processActionCommand(BaseCommandMessage)
    private static final ActionExecutor basicActionExecutor = () -> log;
    // Executor services for background processing
    private ExecutorService controlExecutorService;
    private ExecutorService operationalExecutorService;
    // Flag to control the current state of the service
    private final AtomicBoolean serviceActive = new AtomicBoolean(false);
    // Flags to control the current state of the processors
    private final AtomicBoolean requestsProcessorActive = new AtomicBoolean(false);
    private final AtomicBoolean responsesProcessorActive = new AtomicBoolean(false);
    // The map of messages in progress, key is correlationId
    private final ConcurrentMap<String, MessageProgressWatchdog<?>> messageInProgress = new ConcurrentHashMap<>();
    // Create income message queue
    private final BlockingQueue<BaseCommandMessage<?>> requests = new LinkedBlockingQueue<>();
    // Create outcome message queue
    private final BlockingQueue<BaseCommandMessage<?>> responses = new LinkedBlockingQueue<>();
    private MessagesProcessorAdapter inputProcessor = null;
    private MessagesProcessorAdapter outputProcessor = null;

    /**
     * Start background processors for requests
     */
    @PostConstruct
    @Override
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
        // preparing launch command context requests processor
        inputProcessor = new RequestsProcessor(serviceActive, operationalExecutorService, log);
        final Runnable requestsProcessor = () -> {
            // requests processor is going to start
            latchOfProcessors.countDown();
            // running requests processor
            inputProcessor.processing();
        };
        //
        // preparing launch command context responses processor
        outputProcessor = new ResponsesProcessor(serviceActive, operationalExecutorService, log);
        final Runnable responsesProcessor = () -> {
            // responses processor is going to start
            latchOfProcessors.countDown();
            // running responses processor
            outputProcessor.processing();
        };
        //
        // launching messages processors
        CompletableFuture.runAsync(requestsProcessor, controlExecutorService);
        CompletableFuture.runAsync(responsesProcessor, controlExecutorService);
        //
        // waiting for processors' start
        waitFor(latchOfProcessors);
        //
        // the command through messages service is started
        log.info("MessageProcessingService Local Version is started.");
    }

    /**
     * Stop background processors for requests
     */
    @PreDestroy
    @Override
    public void shutdown() {
        if (!serviceActive.get()) {
            log.warn("MessageProcessingService Local Version is already stopped.");
            return;
        }
        // clear main flag of the service
        serviceActive.getAndSet(false);
        // send to queues final messages
        shutdownQueuesProcessing();
        // wait for processors services are active
        while (requestsProcessorActive.get() || responsesProcessorActive.get()) {
            Thread.yield();
        }
        // shutdown processors executors
        shutdown(operationalExecutorService);
        shutdown(controlExecutorService);
        controlExecutorService = null;
        operationalExecutorService = null;
        log.info("MessageProcessingService Local Version is stopped.");
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

    /**
     * To send command context message for processing
     *
     * @param message the command message to be sent
     * @param <T>     type of command execution result
     * @see this#sendToRequestsQueue(BaseCommandMessage)
     * @see BaseCommandMessage
     */
    @Override
    public <T> void send(final BaseCommandMessage<T> message) {
        final String messageCorrelationId = message.getCorrelationId();
        if (!requestsProcessorActive.get()) {
            final String commandId = message.getContext().getCommand().getId();
            log.warn("RequestMessagesProcessor is NOT active. Message with correlationId='{}' is NOT sent for processing", messageCorrelationId);
            ActionFacade.throwFor(commandId, new IllegalStateException("RequestMessagesProcessor is NOT active."));
        }
        // put message-watcher to in-progress map by correlation-id
        if (messageInProgress.putIfAbsent(messageCorrelationId, new MessageProgressWatchdog<>()) != null) {
            log.warn("Message with correlationId='{}' is already in progress", messageCorrelationId);
        } else {
            if (sendToRequestsQueue(message)) {
                log.info("Message with correlationId='{}' is sent for processing", messageCorrelationId);
            } else {
                log.error("Message with correlationId='{}' is NOT added to processing queue", messageCorrelationId);
            }
        }
    }

    /**
     * To process the request message's processing command in new transaction (strong isolation)<BR/>
     * and send the response to the responses queue
     *
     * @param message message to process
     * @see ActionExecutor#processActionCommand(BaseCommandMessage)
     */
    @Override
    public void processActionCommandAndProceed(final BaseCommandMessage<?> message) {
        final String correlationId = message.getCorrelationId();
        log.debug("Processing request message with correlationId='{}'", correlationId);
        // check if the request in progress map by correlation-id
        if (!messageInProgress.containsKey(correlationId)) {
            logMessageIsNotInProgress(correlationId);
            return;
        }
        // process the request's command locally and send the result to the responses queue
        final BaseCommandMessage<?> responseMessage = basicActionExecutor.processActionCommand(message);
        log.debug("Processed request message with correlationId='{}'", correlationId);
        // try to send result to the responses queue
        if (sendToResponsesQueue(responseMessage)) {
            // successfully sent
            log.debug("Message with correlationId='{}' is processed and sent to responses queue", correlationId);
        } else {
            // something went wrong
            log.error("Message with correlationId='{}' is processed but NOT added to responses queue", correlationId);
        }
    }

    /**
     * To receive processed command message (sent by send(message)) by correlationId
     *
     * @param commandId            the command id of the command in the message
     * @param messageCorrelationId the correlation id to find processed command message
     * @return the processed command message
     * @see RequestsProcessor#processActionCommandAndProceed(BaseCommandMessage)
     * @see BaseCommandMessage
     * @see MessageProgressWatchdog
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> BaseCommandMessage<T> receive(String commandId, String messageCorrelationId) {
        if (!responsesProcessorActive.get()) {
            log.warn("ResponseMessagesProcessor is NOT active. Message with correlationId='{}' is NOT received", messageCorrelationId);
            return ActionFacade.throwFor(commandId, new IllegalStateException("ResponseMessagesProcessor is NOT active."));
        }
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
        final BaseCommandMessage<T> result = messageWatcher.getResult();
        log.info("The result of command '{}' is {}", commandId, result);
        return result;
    }

    /**
     * To finish processing of queues
     */
    protected void shutdownQueuesProcessing() {
        // send empty messages to unblock queues if they are waiting
        try {
            requests.put(BaseCommandMessage.EMPTY);
            responses.put(BaseCommandMessage.EMPTY);
        } catch (InterruptedException e) {
            log.warn("Interrupted while finishing queues processing.", e);
            /* Clean up whatever needs to be handled before interrupting  */
            Thread.currentThread().interrupt();
        }
    }

    /**
     * To send command message to requests queue for processing
     *
     * @param message the command message to be sent
     * @return true if successfully added to requests queue
     * @see this#send(BaseCommandMessage)
     * @see BaseCommandMessage
     */
    protected boolean sendToRequestsQueue(final BaseCommandMessage<?> message) {
        return requests.offer(message);
    }

    /**
     * To take command message from requests queue for further processing
     *
     * @return the command message taken from requests queue
     * @throws InterruptedException if interrupted while waiting
     * @see BaseCommandMessage
     */
    protected BaseCommandMessage<?> takeFromRequestsQueue() throws InterruptedException {
        return requests.take();
    }

    /**
     * To send command message to responses queue for processing result
     *
     * @param message the command message to be sent
     * @return true if successfully added to responses queue
     * @see this#send(BaseCommandMessage)
     * @see BaseCommandMessage
     */
    protected boolean sendToResponsesQueue(final BaseCommandMessage<?> message) {
        return responses.offer(message);
    }

    /**
     * To take command message from responses queue for further processing
     *
     * @return the command message taken from responses queue
     * @throws InterruptedException if interrupted while waiting
     * @see BaseCommandMessage
     */
    protected BaseCommandMessage<?> takeFromResponsesQueue() throws InterruptedException {
        return responses.take();
    }

    // private methods
    // log that message with correlationId is not found in progress map
    private static void logMessageIsNotInProgress(final String correlationId) {
        log.warn("= Message with correlationId='{}' is NOT found in progress map", correlationId);
    }

    // inner classes

    // Messages processor for background processing of requests
    private class RequestsProcessor extends MessagesProcessorAdapter {
        private RequestsProcessor(AtomicBoolean active, Executor executor, Logger logger) {
            super(active, executor, logger);
        }

        @Override
        protected String getProcessorName() {
            return "RequestMessagesProcessor";
        }

        @Override
        @SuppressWarnings("unchecked")
        protected BaseCommandMessage<?> takeFromQueue() throws InterruptedException {
            return CommandThroughMessageServiceLocalImpl.this.takeFromRequestsQueue();
        }

        @Override
        protected void processTakenMessage(final BaseCommandMessage<?> message) {
            // setting up processing context for current thread
            ActionContext.install(message.getActionContext());
            try {
                log.info("Start processing request with direction:{} correlation-id:{}", message.getDirection(), message.getCorrelationId());
                // process processing in the transaction
                CommandThroughMessageServiceLocalImpl.this.processActionCommandAndProceed(message);
                log.info(" ++ Successfully processed request with direction:{} correlation-id:{}", message.getDirection(), message.getCorrelationId());
            } catch (Throwable e) {
                log.error("== Cannot process message {}", message.getCorrelationId(), e);
                if (!message.getContext().isFailed()) {
                    log.error("=+= Context not failed but something thrown after {}", message.getContext(), e);
                    if (e instanceof Exception exception) {
                        message.getContext().failed(exception);
                    } else {
                        log.warn("=?= Something strange was thrown =?=", e);
                    }
                }
                log.error("== Sending failed context of message {}", message.getCorrelationId());
                CommandThroughMessageServiceLocalImpl.this.sendToResponsesQueue(message);
            } finally {
                // release current processing context
                ActionContext.release();
            }
        }

        @Override
        protected boolean isProcessorActive() {
            return requestsProcessorActive.get();
        }

        @Override
        protected void activateProcessor() {
            requestsProcessorActive.getAndSet(true);
        }

        @Override
        protected void deActivateProcessor() {
            requestsProcessorActive.getAndSet(false);
        }
    }

    // Messages processor for background processing of responses
    private class ResponsesProcessor extends MessagesProcessorAdapter {
        private ResponsesProcessor(AtomicBoolean active, Executor executor, Logger logger) {
            super(active, executor, logger);
        }

        @Override
        protected String getProcessorName() {
            return "ResponseMessagesProcessor";
        }

        @Override
        @SuppressWarnings("unchecked")
        protected BaseCommandMessage<?> takeFromQueue() throws InterruptedException {
            return takeFromResponsesQueue();
        }

        @Override
        protected void processTakenMessage(BaseCommandMessage<?> message) {
            log.info("Start processing response with correlationId='{}' which is needs completion", message.getCorrelationId());
            completeMessageProcessing(message);
        }

        @Override
        protected boolean isProcessorActive() {
            return responsesProcessorActive.get();
        }

        @Override
        protected void activateProcessor() {
            responsesProcessorActive.getAndSet(true);
        }

        @Override
        protected void deActivateProcessor() {
            responsesProcessorActive.getAndSet(false);
        }

        @SuppressWarnings("unchecked")
        // complete the message processing
        private void completeMessageProcessing(final BaseCommandMessage response) {
            final String correlationId = response.getCorrelationId();
            // check if the message in progress map by correlation-id
            final MessageProgressWatchdog<?> messageWatcher = messageInProgress.get(correlationId);
            if (isNull(messageWatcher)) {
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
