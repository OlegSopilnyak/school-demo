package oleg.sopilnyak.test.service.facade.impl;

import static java.util.Objects.isNull;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Slf4j
public class CommandThroughMessageServiceLocalImpl implements CommandThroughMessageService {
    @Value("${school.maximum.threads.pool.size:10}")
    private int maximumPoolSize;
    // Create executor for processing message commands
    private static final ActionExecutor actionExecutor = () -> log;
    // Create executor services for background processing
    private final ThreadPoolTaskExecutor controlExecutorService = new ThreadPoolTaskExecutor();
    private final ThreadPoolTaskExecutor operationalExecutorService = new ThreadPoolTaskExecutor();
    // Flag to control the current state of the service
    private final AtomicBoolean serviceActive = new AtomicBoolean(false);
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
    public void startService() {
        if (!serviceActive.get()) {
            // adjust background processors
            // control executor for request/response processors
            final int controlPoolSize = 2;
            controlExecutorService.setCorePoolSize(controlPoolSize);
            controlExecutorService.setMaxPoolSize(controlPoolSize);
            controlExecutorService.setThreadNamePrefix("ProcessorControl-");
            controlExecutorService.initialize();

            // operational executor for processing commands
            final int operationalPoolSize = Math.max(maximumPoolSize, Runtime.getRuntime().availableProcessors());
            operationalExecutorService.setCorePoolSize(operationalPoolSize);
            operationalExecutorService.setMaxPoolSize(operationalPoolSize);
            operationalExecutorService.setThreadNamePrefix("QueueMessageProcessor-");
            operationalExecutorService.initialize();

            // starting background processors
            serviceActive.getAndSet(true);

            // launch messages processors
            controlExecutorService.execute(new RequestProcessor(serviceActive, operationalExecutorService));
            controlExecutorService.execute(new ResponseProcessor(serviceActive, operationalExecutorService));

            // the service is started
            log.info("MessageProcessingService Local Version is started.");
        }
    }

    /**
     * Stop background processors for requests
     */
    @PreDestroy
    public void stopService() {
        if (serviceActive.get()) {
            serviceActive.getAndSet(false);
            shutdownQueuesProcessing();
            while (controlExecutorService.getActiveCount() > 0) {
                Thread.yield();
            }
            controlExecutorService.shutdown();
            operationalExecutorService.shutdown();
            log.info("MessageProcessingService Local Version is stopped.");
        }
    }

    /**
     * To send command message for processing
     *
     * @param message the command message to be sent
     * @see this#sendToRequestsQueue(BaseCommandMessage)
     * @see BaseCommandMessage
     */
    @Override
    public <T> void send(final BaseCommandMessage<T> message) {
        if (messageInProgress.putIfAbsent(message.getCorrelationId(), new MessageInProgress<>()) != null) {
            log.warn("Message with correlationId='{}' is already in progress", message.getCorrelationId());
        } else {
            if (sendToRequestsQueue(message)) {
                log.info("Message with correlationId='{}' is sent for processing", message.getCorrelationId());
            } else {
                log.error("Message with correlationId='{}' is NOT added to processing queue", message.getCorrelationId());
            }
        }
    }

    /**
     * To receive processed command message (sent by send(message)) by correlationId
     *
     * @param correlationId the correlation id to find processed command message
     * @return the processed command message
     * @see RequestProcessor#processActionCommandAndSendResponse(BaseCommandMessage)
     * @see BaseCommandMessage
     * @see MessageInProgress
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> BaseCommandMessage<T> receive(String correlationId) {
        final MessageInProgress<T> messageWatcher = (MessageInProgress<T>) messageInProgress.get(correlationId);
        if (isNull(messageWatcher)) {
            logMessageIsNotInProgress(correlationId);
            return null;
        }
        // wait until processing is completed
        messageWatcher.waitForMessageComplete();
        // remove message-watcher from in-progress map by correlation-id
        messageInProgress.remove(correlationId);
        // return the result
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
        log.warn("Message with correlationId='{}' is NOT found in progress map", correlationId);
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

    // Abstract runnable for background processing of messages
    private abstract static class MessageProcessor implements Runnable {
        final AtomicBoolean active;
        private final ThreadPoolTaskExecutor executor;

        private MessageProcessor(AtomicBoolean active, ThreadPoolTaskExecutor executor) {
            this.active = active;
            this.executor = executor;
        }

        @Override
        public void run() {
            log.info("{} is started. Main service active = '{}'", getProcessorName(), active);
            while (active.get()) {
                try {
                    final BaseCommandMessage<?> message = takeFromQueue();
                    if (active.get() && !Objects.equals(message, BaseCommandMessage.EMPTY)) {
                        CompletableFuture.runAsync(() -> processTakenMessage(message), executor);
                    }
                } catch (InterruptedException e) {
                    log.warn("{} getting command requests is interrupted", getProcessorName(), e);
                    /* Clean up whatever needs to be handled before interrupting  */
                    Thread.currentThread().interrupt();
                }
            }

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
    }

    // Runnable for background processing of requests
    private class RequestProcessor extends MessageProcessor {

        private RequestProcessor(AtomicBoolean active, ThreadPoolTaskExecutor executor) {
            super(active, executor);
        }

        /**
         * Get the name of the processor for logging purposes.
         *
         * @return the name of the processor
         */
        @Override
        protected String getProcessorName() {
            return "RequestProcessor";
        }

        /**
         * To take command message from the appropriate queue for further processing
         *
         * @return the command message taken from the appropriate queue
         * @throws InterruptedException if interrupted while waiting
         * @see BaseCommandMessage
         */
        @Override
        protected BaseCommandMessage<?> takeFromQueue() throws InterruptedException {
            return takeFromRequestsQueue();
        }

        /**
         * To process the taken message.
         *
         * @param message the command message to be processed
         */
        @Override
        protected void processTakenMessage(BaseCommandMessage<?> message) {
            processActionCommandAndSendResponse(message);
        }

        // process the request's action command and send the response to responses queue
        private void processActionCommandAndSendResponse(final BaseCommandMessage<?> request) {
            final String correlationId = request.getCorrelationId();
            log.debug("Processing request message with correlationId='{}'", correlationId);
            // check if the message in progress map by correlation-id
            if (!messageInProgress.containsKey(correlationId)) {
                logMessageIsNotInProgress(correlationId);
                return;
            }
            // Create and send the response
            if (sendToResponsesQueue(actionExecutor.processActionCommand(request))) {
                log.debug("Message with correlationId='{}' is processed and sent to responses queue", correlationId);
            } else {
                log.error("Message with correlationId='{}' is processed but NOT added to responses queue", correlationId);
            }
        }
    }

    // Runnable for background processing of responses
    private class ResponseProcessor extends MessageProcessor {

        private ResponseProcessor(AtomicBoolean active, ThreadPoolTaskExecutor executor) {
            super(active, executor);
        }

        /**
         * Get the name of the processor for logging purposes.
         *
         * @return the name of the processor
         */
        @Override
        protected String getProcessorName() {
            return "ResponseProcessor";
        }

        /**
         * To take command message from the appropriate queue for further processing
         *
         * @return the command message taken from the appropriate queue
         * @throws InterruptedException if interrupted while waiting
         * @see BaseCommandMessage
         */
        @Override
        protected BaseCommandMessage<?> takeFromQueue() throws InterruptedException {
            return takeFromResponsesQueue();
        }

        /**
         * To process the taken message.
         *
         * @param message the command message to be processed
         */
        @Override
        protected void processTakenMessage(BaseCommandMessage<?> message) {
            completeMessageProcessing(message);
        }

        // complete the message processing
        private void completeMessageProcessing(final BaseCommandMessage response) {
            final String correlationId = response.getCorrelationId();
            log.info("Processing response with correlationId='{}' is ready for complete", correlationId);
            // check if the message in progress map by correlation-id
            final MessageInProgress messageWatcher = messageInProgress.get(correlationId);
            if (isNull(messageWatcher)) {
                logMessageIsNotInProgress(correlationId);
                return;
            }
            // set up the result and mark as completed
            messageWatcher.result = response;
            messageWatcher.state = State.COMPLETED;
            messageWatcher.messageProcessingIsDone();

        }
    }
}
