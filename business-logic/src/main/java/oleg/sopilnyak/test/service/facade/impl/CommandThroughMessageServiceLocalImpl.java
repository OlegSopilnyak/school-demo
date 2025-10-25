package oleg.sopilnyak.test.service.facade.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.exception.CountDownLatchInterruptedException;
import oleg.sopilnyak.test.service.facade.ActionFacade;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.util.Objects.isNull;

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
    private static final ActionExecutor actionExecutor = () -> log;
    // Executor services for background processing
    private ThreadPoolTaskExecutor controlExecutorService;
    private ThreadPoolTaskExecutor operationalExecutorService;
    // Flag to control the current state of the service
    private final AtomicBoolean serviceActive = new AtomicBoolean(false);
    // Flags to control the current state of the processors
    private final AtomicBoolean requestsProcessorActive = new AtomicBoolean(false);
    private final AtomicBoolean responsesProcessorActive = new AtomicBoolean(false);
    // The map of messages in progress, key is correlationId
    private final ConcurrentMap<String, MessageInProgress<?>> messageInProgress = new ConcurrentHashMap<>();
    // Create income message queue
    private final BlockingQueue<BaseCommandMessage<?>> requests = new LinkedBlockingQueue<>();
    // Create outcome message queue
    private final BlockingQueue<BaseCommandMessage<?>> responses = new LinkedBlockingQueue<>();

    /**
     * Start background processors for requests
     */
    @PostConstruct
    @Override
    public void initialize() {
        if (!serviceActive.get()) {
            // adjust background processors
            // control executor for request/response processors
            controlExecutorService = new ThreadPoolTaskExecutor();
            final int controlPoolSize = 2;
            controlExecutorService.setCorePoolSize(controlPoolSize);
            controlExecutorService.setMaxPoolSize(controlPoolSize);
            controlExecutorService.setThreadNamePrefix("ProcessorControl-");
            controlExecutorService.initialize();

            // operational executor for processing command messages
            operationalExecutorService = new ThreadPoolTaskExecutor();
            final int operationalPoolSize = Math.max(maximumPoolSize, Runtime.getRuntime().availableProcessors());
            operationalExecutorService.setCorePoolSize(operationalPoolSize);
            operationalExecutorService.setMaxPoolSize(operationalPoolSize);
            operationalExecutorService.setThreadNamePrefix("QueueMessageProcessor-");
            operationalExecutorService.initialize();

            // starting background processors
            serviceActive.getAndSet(true);

            // launch command messages processors
            final CountDownLatch processorStarted = new CountDownLatch(2);
            //
            // launching command context requests processor
            CompletableFuture.runAsync(() -> {
                // requests processor is going to start
                processorStarted.countDown();
                // running requests processor
                new RequestProcessor(serviceActive, operationalExecutorService).action();
            }, controlExecutorService);
            //
            // launching command context responses processor
            CompletableFuture.runAsync(() -> {
                // responses processor is going to start
                processorStarted.countDown();
                // running responses processor
                new ResponseProcessor(serviceActive, operationalExecutorService).action();
            }, controlExecutorService);
            //
            // waiting processors' starting
            try {
                processorStarted.await();
            } catch (InterruptedException e) {
                /* Clean up whatever needs to be handled before interrupting  */
                Thread.currentThread().interrupt();
                throw new CountDownLatchInterruptedException(2, e);
            }
            //
            // the command through messages service is started
            log.info("MessageProcessingService Local Version is started.");
        } else {
            log.warn("MessageProcessingService Local Version is already started.");
        }
    }

    /**
     * Stop background processors for requests
     */
    @PreDestroy
    @Override
    public void shutdown() {
        if (serviceActive.get()) {
            // clear main flag of the service
            serviceActive.getAndSet(false);
            // send to queues final messages
            shutdownQueuesProcessing();
            // wait for processors services are active
            while (requestsProcessorActive.get() || responsesProcessorActive.get()) {
                Thread.yield();
            }
            // shutdown processors executors
            controlExecutorService.shutdown();
            operationalExecutorService.shutdown();
            controlExecutorService = null;
            operationalExecutorService = null;
            log.info("MessageProcessingService Local Version is stopped.");
        } else {
            log.warn("MessageProcessingService Local Version is already stopped.");
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
        if (messageInProgress.putIfAbsent(messageCorrelationId, new MessageInProgress<>()) != null) {
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
     * To process the request message's action command in new transaction (strong isolation)<BR/>
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
        final BaseCommandMessage<?> responseMessage = actionExecutor.processActionCommand(message);
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
     * @see RequestProcessor#processActionCommandAndProceed(BaseCommandMessage)
     * @see BaseCommandMessage
     * @see MessageInProgress
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> BaseCommandMessage<T> receive(String commandId, String messageCorrelationId) {
        if (!responsesProcessorActive.get()) {
            log.warn("ResponseMessagesProcessor is NOT active. Message with correlationId='{}' is NOT received", messageCorrelationId);
            return ActionFacade.throwFor(commandId, new IllegalStateException("ResponseMessagesProcessor is NOT active."));
        }
        // getting message-watcher from in-progress map by correlation-id
        final MessageInProgress<T> messageWatcher = (MessageInProgress<T>) messageInProgress.get(messageCorrelationId);
        if (isNull(messageWatcher)) {
            logMessageIsNotInProgress(messageCorrelationId);
            return null;
        }
        // wait until processing is completed
        messageWatcher.waitForMessageComplete();
        // remove message-watcher from in-progress map by correlation-id
        messageInProgress.remove(messageCorrelationId);
        // return the result
        log.info("The result of command '{}' is {}", commandId, messageWatcher.result);
        return messageWatcher.result;
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
    // State of message processing
    enum State {IN_PROGRESS, COMPLETED}

    // wrapper for processing the message
    protected static class MessageInProgress<T> {
        @Getter
        private BaseCommandMessage<T> result;
        @Getter
        private volatile State state = State.IN_PROGRESS;
        private final Object getResultMonitor = new Object();

        void waitForMessageComplete() {
            synchronized (getResultMonitor) {
                while (state == State.IN_PROGRESS) {
                    try {
                        getResultMonitor.wait(100);
                    } catch (InterruptedException e) {
                        log.warn("Interrupted while waiting for state to complete.", e);
                        /* Clean up whatever needs to be handled before interrupting  */
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        void messageProcessingIsDone() {
            synchronized (getResultMonitor) {
                if (state == State.COMPLETED) {
                    getResultMonitor.notifyAll();
                }
            }
        }
    }

    // Abstract messages processor for background processing of command-execution  messages
    private abstract static class MessagesProcessor {
        private final AtomicBoolean active;
        private final ThreadPoolTaskExecutor executor;

        private MessagesProcessor(AtomicBoolean active, ThreadPoolTaskExecutor executor) {
            this.active = active;
            this.executor = executor;
        }

        /**
         * To run messages processor execution
         */
        void action() {
            if (isProcessorActive()) {
                log.warn("{} is already running.", getProcessorName());
                return;
            }
            // mark as active
            activateProcessor();
            log.info("{} is started. Main service active = '{}'", getProcessorName(), active);
            while (active.get()) {
                try {
                    final BaseCommandMessage<?> message = takeFromQueue();
                    if (active.get() && !Objects.equals(message, BaseCommandMessage.EMPTY)) {
                        // process the message asynchronously if not empty
                        CompletableFuture.runAsync(() -> processTakenMessage(message), executor);
                    }
                } catch (InterruptedException e) {
                    log.warn("{} getting command requests is interrupted", getProcessorName(), e);
                    /* Clean up whatever needs to be handled before interrupting  */
                    Thread.currentThread().interrupt();
                }
            }
            // mark as inactive
            deActivateProcessor();
        }

        /**
         * Get the name of the processor for logging purposes.
         *
         * @return the name of the processor
         */
        protected abstract String getProcessorName();

        /**
         * To take command message from the appropriate queue for further processing
         *
         * @return the command message taken from the appropriate queue
         * @throws InterruptedException if interrupted while waiting
         * @see BaseCommandMessage
         */
        protected abstract <T> BaseCommandMessage<T> takeFromQueue() throws InterruptedException;

        /**
         * To process the taken message.
         *
         * @param message the command message to be processed
         */
        protected abstract void processTakenMessage(BaseCommandMessage<?> message);

        /**
         * To check if the processor is active
         *
         * @return true if the processor is active
         */
        protected abstract boolean isProcessorActive();

        /**
         * To activate the processor
         */
        protected abstract void activateProcessor();

        /**
         * To deactivate the processor
         */
        protected abstract void deActivateProcessor();
    }

    // Messages processor for background processing of requests
    private class RequestProcessor extends MessagesProcessor {
        private RequestProcessor(AtomicBoolean active, ThreadPoolTaskExecutor executor) {
            super(active, executor);
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
            // set up action context for current thread
            ActionContext.install(message.getActionContext());
            try {
                log.info("Start processing request with direction:{} correlation-id:{}", message.getDirection(), message.getCorrelationId());
                // process action in the transaction
                CommandThroughMessageServiceLocalImpl.this.processActionCommandAndProceed(message);
                log.info("Successfully processed request with direction:{} correlation-id:{}", message.getDirection(), message.getCorrelationId());
            } catch (Throwable e) {
                log.error("== Cannot process message {}", message.getCorrelationId(), e);
            } finally {
                // release current action context
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
    private class ResponseProcessor extends MessagesProcessor {
        private ResponseProcessor(AtomicBoolean active, ThreadPoolTaskExecutor executor) {
            super(active, executor);
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
            final MessageInProgress<?> messageWatcher = messageInProgress.get(correlationId);
            if (isNull(messageWatcher)) {
                logMessageIsNotInProgress(correlationId);
                return;
            }
            // set up the result and mark as completed
            messageWatcher.result = response;
            messageWatcher.state = State.COMPLETED;
            // notify thread waiting for this message-watcher result
            messageWatcher.messageProcessingIsDone();
            log.info("Successfully processed response with correlationId='{}'", correlationId);
        }
    }
}
