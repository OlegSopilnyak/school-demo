package oleg.sopilnyak.test.service.facade.impl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@AllArgsConstructor
public class MessageProcessingServiceLocalImpl implements MessageProcessingService {
    @Value("${school.maximum.threads.pool.size:10}")
    private int maximumPoolSize;
    // Create executor for processing message commands
    private static final ActionExecutor actionExecutor = () -> log;
    // Create executor service for background processing
    private final ScheduledThreadPoolExecutor executorService = new ScheduledThreadPoolExecutor(1);
    // Flag to control the running state of the service
    private volatile boolean isRunning;
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
        if (!isRunning) {
            // adjust background processors
            executorService.setMaximumPoolSize(Math.max(maximumPoolSize, Runtime.getRuntime().availableProcessors()));
            // start background processors
            isRunning = true;
            executorService.execute(new RequestProcessor());
            executorService.execute(new ResponseProcessor());
            log.info("MessageProcessingService Local Version is started.");
        }
    }

    /**
     * Stop background processors for requests
     */
    @PreDestroy
    public void stopService() {
        if (isRunning) {
            isRunning = false;
            softExecutorShutdown(executorService);
            if (!executorService.isTerminated()) {
                log.warn("MessageProcessingService Local Version is not stopped in time, forcing shutdown.");
                executorService.shutdownNow();
            }
            log.info("MessageProcessingService Local Version is stopped.");
        }
    }

    /**
     * To send command message for processing
     *
     * @param message the command message to be sent
     * @see BaseCommandMessage
     */
    @Override
    public <T> void send(final BaseCommandMessage<T> message) {
        final MessageInProgress<T> messageWatcher = new MessageInProgress<>();
        if (messageInProgress.putIfAbsent(message.getCorrelationId(), messageWatcher) != null) {
            log.warn("Message with correlationId='{}' is already in progress", message.getCorrelationId());
        } else {
            if (requests.offer(message)) {
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
     * @see BaseCommandMessage
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> BaseCommandMessage<T> receive(String correlationId) {
        final MessageInProgress<T> messageWatcher = (MessageInProgress<T>) messageInProgress.get(correlationId);
        if (messageWatcher == null) {
            logMessageIsNotInProgress(correlationId);
            return null;
        } else {
            // wait until processing is completed
            messageWatcher.waitForMessageComplete();
            // remove from in-progress map
            messageInProgress.remove(correlationId);
            // return the result
            return messageWatcher.result;
        }
    }

    // private methods
    private static void logMessageIsNotInProgress(final String correlationId) {
        log.warn("Message with correlationId='{}' is NOT found in progress map", correlationId);
    }

    // try to stop executor service softly
    private static void softExecutorShutdown(final ScheduledThreadPoolExecutor executorService) {
        executorService.shutdown();
        for (int i = 1; i <= 10 && !executorService.isTerminated(); i++) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("MessageProcessingService Local Version stopping is interrupted.");
            }
        }
    }

    // inner classes
    // State of message processing
    enum State {IN_PROGRESS, COMPLETED}

    // wrapper for processing the message
    private static class MessageInProgress<T> {
        private BaseCommandMessage<T> result;
        private final ReentrantLock lock = new ReentrantLock();
        private final Object lockMonitor = new Object();
        private volatile State state = State.IN_PROGRESS;

        void waitForMessageComplete() {
            synchronized (lockMonitor) {
                if (!lock.isLocked() && state == State.IN_PROGRESS) {
                    lock.lock();
                }
            }
        }

        void messageProcessingIsDone() {
            synchronized (lockMonitor) {
                if (lock.isLocked() && state == State.COMPLETED) {
                    lock.unlock();
                }
            }
        }
    }

    // Runnable for background processing of requests
    private class RequestProcessor implements Runnable {
        @Override
        public void run() {
            while (isRunning) {
                final BaseCommandMessage<?> request = requests.poll();
                if (request != null) {
                    executorService.submit(() -> processActionCommandAndSendResponse(request));
                } else {
                    // If the service is stopped while waiting, exit the loop
                    isRunning = false;
                    return;
                }
            }
        }

        // process the request's action command and send the response to responses queue
        @SuppressWarnings("unchecked")
        private <R> void processActionCommandAndSendResponse(final BaseCommandMessage<R> request) {
            final String correlationId = request.getCorrelationId();
            log.debug("Processing message with correlationId='{}'", correlationId);
            // check if the message in progress map
            final MessageInProgress<R> messageWatcher = (MessageInProgress<R>) messageInProgress.get(correlationId);
            if (messageWatcher != null) {
                // Create and send the response
                if (responses.offer(actionExecutor.processActionCommand(request))) {
                    log.debug("Message with correlationId='{}' is processed and sent to responses queue", correlationId);
                } else {
                    log.error("Message with correlationId='{}' is processed but NOT added to responses queue", correlationId);
                }
            } else {
                logMessageIsNotInProgress(correlationId);
            }
        }
    }

    // Runnable for background processing of responses
    private class ResponseProcessor implements Runnable {

        /**
         * Runs this operation.
         */
        @Override
        public void run() {
            while (isRunning) {
                final BaseCommandMessage<?> response = responses.poll();
                if (response != null) {
                    executorService.submit(() -> completeMessageProcessing(response));
                } else {
                    // If the service is stopped while waiting, exit the loop
                    isRunning = false;
                    return;
                }
            }
        }

        // complete the message processing
        @SuppressWarnings("unchecked")
        private <R> void completeMessageProcessing(final BaseCommandMessage<R> response) {
            final String correlationId = response.getCorrelationId();
            log.debug("Response with correlationId='{}' is ready for complete", correlationId);
            // check if the message in progress map
            final MessageInProgress<R> messageWatcher = (MessageInProgress<R>) messageInProgress.get(correlationId);
            if (messageWatcher != null) {
                // set up the result and mark as completed
                messageWatcher.result = response;
                messageWatcher.state = State.COMPLETED;
                messageWatcher.messageProcessingIsDone();
            } else {
                logMessageIsNotInProgress(correlationId);
            }
        }

    }
}
