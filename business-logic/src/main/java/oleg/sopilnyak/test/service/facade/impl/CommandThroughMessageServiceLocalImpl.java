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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.facade.ActionFacade;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Slf4j
@RequiredArgsConstructor
@Service
public class CommandThroughMessageServiceLocalImpl implements CommandThroughMessageService {
    // transactions support manager
    private final PlatformTransactionManager ptm;

    @Value("${school.maximum.threads.pool.size:10}")
    private int maximumPoolSize;
    // Create executor for processing message commands
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

            // operational executor for processing commands
            operationalExecutorService = new ThreadPoolTaskExecutor();
            final int operationalPoolSize = Math.max(maximumPoolSize, Runtime.getRuntime().availableProcessors());
            operationalExecutorService.setCorePoolSize(operationalPoolSize);
            operationalExecutorService.setMaxPoolSize(operationalPoolSize);
            operationalExecutorService.setThreadNamePrefix("QueueMessageProcessor-");
            operationalExecutorService.initialize();

            // starting background processors
            serviceActive.getAndSet(true);

            // launch messages processors
            controlExecutorService.execute(new RequestProcessor(serviceActive, operationalExecutorService, ptm));
            controlExecutorService.execute(new ResponseProcessor(serviceActive, operationalExecutorService, ptm));
            // waiting for processors to be started
            while (!requestsProcessorActive.get() || !responsesProcessorActive.get()) {
                Thread.yield();
            }

            // the service is started
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
            serviceActive.getAndSet(false);
            shutdownQueuesProcessing();
            while (requestsProcessorActive.get() || responsesProcessorActive.get()) {
                Thread.yield();
            }
            controlExecutorService.shutdown();
            operationalExecutorService.shutdown();
            log.info("MessageProcessingService Local Version is stopped.");
        } else {
            log.warn("MessageProcessingService Local Version is already stopped.");
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
     * To process the request message's action command in new transaction
     * and send the response to responses queue
     *
     * @param requestMessage message to process
     */
    @Override
    public void processActionCommandAndProceed(final BaseCommandMessage<?> requestMessage) {
        final String correlationId = requestMessage.getCorrelationId();
        log.debug("Processing request message with correlationId='{}'", correlationId);
        // check if the request in progress map by correlation-id
        if (!messageInProgress.containsKey(correlationId)) {
            logMessageIsNotInProgress(correlationId);
            return;
        }
        // process the request's command and send the result responses queue
        final BaseCommandMessage<?> responseMessage = actionExecutor.processActionCommand(requestMessage);
        log.debug("Processed request message with correlationId='{}'", correlationId);
        if (sendToResponsesQueue(responseMessage)) {
            log.debug("Message with correlationId='{}' is processed and sent to responses queue", correlationId);
        } else {
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

    // Abstract runnable for background processing of messages
    private abstract static class MessageProcessor implements Runnable {
        protected final PlatformTransactionManager platformTransactionManager;
        final AtomicBoolean active;
        private final ThreadPoolTaskExecutor executor;

        private MessageProcessor(AtomicBoolean active, ThreadPoolTaskExecutor executor, PlatformTransactionManager platformTransactionManager) {
            this.active = active;
            this.executor = executor;
            this.platformTransactionManager = platformTransactionManager;
        }

        @Override
        public void run() {
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

    // private methods
    // make transactio definition for command action processing
    private static DefaultTransactionDefinition transactionDefinitionFor(BaseCommandMessage<?> message) {
        final DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();
        // explicitly setting the transaction name is something that can be done only programmatically
        final String actionContext = message.getActionContext().getFacadeName() + ":" + message.getActionContext().getActionName();
        transactionDefinition.setName("Transaction for action " + actionContext);
        transactionDefinition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transactionDefinition;
    }

    // Runnable for background processing of requests
    private class RequestProcessor extends MessageProcessor {

        private RequestProcessor(AtomicBoolean active, ThreadPoolTaskExecutor executor, PlatformTransactionManager platformTransactionManager) {
            super(active, executor, platformTransactionManager);
        }

        /**
         * Get the name of the processor for logging purposes.
         *
         * @return the name of the processor
         */
        @Override
        protected String getProcessorName() {
            return "RequestMessagesProcessor";
        }

        /**
         * To take command message from the appropriate queue for further processing
         *
         * @return the command message taken from the appropriate queue
         * @throws InterruptedException if interrupted while waiting
         * @see BaseCommandMessage
         */
        @Override
        @SuppressWarnings("unchecked")
        protected BaseCommandMessage<?> takeFromQueue() throws InterruptedException {
            return takeFromRequestsQueue();
        }

        /**
         * To process the taken message.
         *
         * @param message the command message to be processed
         */
        @Override
        protected void processTakenMessage(final BaseCommandMessage<?> message) {
            // set up action context for current thread
            ActionContext.install(message.getActionContext());
            // prepare the transaction
            final DefaultTransactionDefinition transactionDefinition = transactionDefinitionFor(message);
            final TransactionStatus transaction = platformTransactionManager.getTransaction(transactionDefinition);
            try {
                log.info("Start processing request with direction:{} correlation-id:{}", message.getDirection(), message.getCorrelationId());
                log.info("The processing request uses transaction:{}", transaction);
                processActionCommandAndProceed(message);
                log.info("Successfully processed request with direction:{} correlation-id:{}", message.getDirection(), message.getCorrelationId());
                platformTransactionManager.commit(transaction);
            } catch (Throwable e) {
                platformTransactionManager.rollback(transaction);
                log.error("== Cannot process message {}", message.getCorrelationId(), e);
            } finally {
                // release current action context
                ActionContext.release();
            }
        }
//        protected void processTakenMessage(final BaseCommandMessage<?> message) {
//            final DefaultTransactionDefinition transactionDefinition = transactionDefinitionFor(message);
//            final TransactionStatus transaction = platformTransactionManager.getTransaction(transactionDefinition);
//            try {
//                log.info("Start processing request with direction:{} correlation-id:{}", message.getDirection(), message.getCorrelationId());
//                processActionCommandAndProceed(message);
//                log.info("Successfully processed request with direction:{} correlation-id:{}", message.getDirection(), message.getCorrelationId());
//                platformTransactionManager.commit(transaction);
//            } catch (Throwable e) {
//                platformTransactionManager.rollback(transaction);
//                log.error("== Cannot process message {}", message.getCorrelationId(), e);
//            }
//        }

        /**
         * To check if the processor is active
         *
         * @return true if the processor is active
         */
        @Override
        protected boolean isProcessorActive() {
            return requestsProcessorActive.get();
        }

        /**
         * To activate the processor
         */
        @Override
        protected void activateProcessor() {
            requestsProcessorActive.getAndSet(true);
        }

        /**
         * To deactivate the processor
         */
        @Override
        protected void deActivateProcessor() {
            requestsProcessorActive.getAndSet(false);
        }
    }

    // Runnable for background processing of responses
    private class ResponseProcessor extends MessageProcessor {

        private ResponseProcessor(AtomicBoolean active, ThreadPoolTaskExecutor executor, PlatformTransactionManager platformTransactionManager) {
            super(active, executor, platformTransactionManager);
        }

        /**
         * Get the name of the processor for logging purposes.
         *
         * @return the name of the processor
         */
        @Override
        protected String getProcessorName() {
            return "ResponseMessagesProcessor";
        }

        /**
         * To take command message from the appropriate queue for further processing
         *
         * @return the command message taken from the appropriate queue
         * @throws InterruptedException if interrupted while waiting
         * @see BaseCommandMessage
         */
        @Override
        @SuppressWarnings("unchecked")
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
            log.info("Start processing response with correlationId='{}' is ready for complete", message.getCorrelationId());
            completeMessageProcessing(message);
        }

        /**
         * To check if the processor is active
         *
         * @return true if the processor is active
         */
        @Override
        protected boolean isProcessorActive() {
            return responsesProcessorActive.get();
        }

        /**
         * To activate the processor
         */
        @Override
        protected void activateProcessor() {
            responsesProcessorActive.getAndSet(true);
        }

        /**
         * To deactivate the processor
         */
        @Override
        protected void deActivateProcessor() {
            responsesProcessorActive.getAndSet(false);
        }

        // complete the message processing
        @SuppressWarnings("unchecked")
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
            messageWatcher.messageProcessingIsDone();
            log.info("Successfully processed response with correlationId='{}'", correlationId);
        }
    }
}
