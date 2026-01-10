package oleg.sopilnyak.test.service.command.executable.core.executor.messaging;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.message.CommandMessage;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service: service to support command-messages exchange
 */
public abstract class MessagesExchange {
    // @see CommandActionExecutor#processActionCommand(CommandMessage)
    private static final Logger logger = LoggerFactory.getLogger("Low Level Command Action Executor");
    private static final CommandActionExecutor lowLevelActionExecutor = () -> logger;
    // The map of messages in progress, key is correlationId
    private final ConcurrentMap<String, MessageProgressWatchdog<?>> messageInProgress = new ConcurrentHashMap<>();

    /**
     * To check the state of messages exchange sub-service
     *
     * @return true if it's active
     */
    public abstract boolean isActive();

    /**
     * To get access to responses messages processor
     *
     * @return the reference of processor
     */
    public abstract MessagesProcessor getResponsesProcessor();

    /**
     * To get access to exchange's logger
     *
     * @return reference to concrete
     */
    protected abstract Logger getLogger();

    /**
     * To prepare message watcher for the command-message
     *
     * @param correlationId correlation-id of message to watch after
     * @param original      original message to watch after
     * @return true if it's made
     */
    public boolean makeMessageInProgress(final String correlationId, final CommandMessage<?> original) {
        return messageInProgress.putIfAbsent(correlationId, new MessageProgressWatchdog<>(original)) == null;
    }

    /**
     * To stop watching after of the command-message
     *
     * @param correlationId correlation-id of message to stop watching after
     */
    public void stopWatchingMessage(String correlationId) {
        messageInProgress.remove(correlationId);
    }

    /**
     * To get the watcher of in-progress message
     *
     * @param correlationId correlation-id of watching message
     * @param <T>           the type of message-command's result
     * @return command-message watcher
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<MessageProgressWatchdog<T>> messageWatchdogFor(String correlationId) {
        return Optional.ofNullable((MessageProgressWatchdog<T>) messageInProgress.get(correlationId));
    }

    /**
     * To process locally the command-message in the action-context
     *
     * @param request command-message to process
     * @see ActionContext
     * @see this#onErrorRequestMessage(CommandMessage, Throwable)
     * @see this#onTakenRequestMessage(CommandMessage)
     */
    protected void executeWithActionContext(CommandMessage<?> request) {
        // checking up request's action-context
        final ActionContext requestActionContext = request.getActionContext();
        final String correlationId = request.getCorrelationId();
        if (requestActionContext == null) {
            getLogger().error("== Couldn't process message request with correlation-id:{} action-context is empty", correlationId);
            onErrorRequestMessage(request, new IllegalArgumentException("Action context must not be null"));
        } else {
            getLogger().debug("Executing request with correlation-id:{} in action-context:{}", correlationId, requestActionContext);
            // setting up processing action-context for the working thread
            ActionContext.install(requestActionContext);
            try {
                final CommandMessage.Direction direction = request.getDirection();
                getLogger().debug("Starting request's processing with direction:{} correlation-id:{}", direction, correlationId);
                // process taken message in the data transaction
                onTakenRequestMessage(request);
                getLogger().debug("++ Successfully processed request with direction:{} correlation-id:{}", direction, correlationId);
            } catch (Exception e) {
                // process message after the error has thrown
                getLogger().error("== Couldn't process message request with correlation-id:{}", correlationId, e);
                onErrorRequestMessage(request, e);
            } finally {
                // release current processing context
                ActionContext.release();
            }
        }
    }

    /**
     * To process the request message's command in the new transaction (strong isolation)<BR/>
     * and send the response to the responses messages-processor
     *
     * @param message command-message to be processed
     * @see this#executeWithActionContext(CommandMessage)
     * @see this#localExecutionResult(CommandMessage)
     * @see this#passProcessedMessageOut(CommandMessage, String)
     * @see MessagesProcessor#accept(CommandMessage)
     */
    public <T> void onTakenRequestMessage(final CommandMessage<T> message) {
        final String correlationId = message.getCorrelationId();
        getLogger().debug("Processing request message with correlationId='{}'", correlationId);
        // processing request using message-in-progress map by correlation-id
        messageWatchdogFor(correlationId).ifPresentOrElse(_ -> {
                    //
                    // process the request's command locally and pass the result to the responses messages-processor
                    final CommandMessage<T> result = localExecutionResult(message);
                    getLogger().debug("Processed request message with correlationId='{}'", correlationId);
                    //
                    // finalize message's processing
                    passProcessedMessageOut(result, correlationId);
                },
                // no command-message-watcher in message-in-progress map
                () -> logMessageIsNotInProgress(correlationId)
        );
    }

    /**
     * Processing (locally) an error of taken request command-message
     *
     * @param message request command-message
     * @param error   cause of command-message processing error
     * @see this#executeWithActionContext(CommandMessage)
     * @see this#passProcessedMessageOut(CommandMessage, String)
     * @see MessagesProcessor#accept(CommandMessage)
     */
    public <T> void onErrorRequestMessage(final CommandMessage<T> message, final Throwable error) {
        if (message.getContext().isFailed()) {
            final String correlationId = message.getCorrelationId();
            getLogger().error("== Sending failed context of message {}", message.getCorrelationId());
            // finalize message's processing
            passProcessedMessageOut(message, correlationId);
        } else {
            getLogger().error("=+= Context not failed but something thrown after {}", message.getContext(), error);
            if (error instanceof Exception exception) {
                message.getContext().failed(exception);
            } else {
                getLogger().warn("=?= Something strange was thrown =?=", error);
                message.getContext().failed(new IllegalArgumentException("Unknown type of error", error));
            }
        }
    }

    /**
     * To process the taken response command-message. Last step of command-message processing.
     *
     * @param message the command message to be finalized
     */
    public <T> void onTakenResponseMessage(final CommandMessage<T> message) {
        final String correlationId = message.getCorrelationId();
        getLogger().info("Finishing processing response with correlationId='{}' which is needs completion", correlationId);
        // getting in-progress-message watchdog form progress map by correlation-id
        final Optional<MessageProgressWatchdog<T>> watchdogOptional = messageWatchdogFor(correlationId);
        // save result to watchdog and notify waiting threads
        watchdogOptional.ifPresentOrElse(watchDog -> {
                    // set up the result and mark as completed
                    watchDog.setResult(message);
                    // notify thread waiting for this message-watcher result
                    // @see CommandMessagesExchangeExecutorAdapter#retrieveProcessedMessage(command-id,correlation-id)
                    watchDog.messageProcessingIsDone();
                    getLogger().info("Successfully processed response with correlationId='{}'", correlationId);
                },
                () -> logMessageIsNotInProgress(correlationId)
        );
    }


    /**
     * To process in-progress command-message and make result of the command as a command-message using low-level action executor
     *
     * @param commandMessage command-message to process
     * @param <T>            type of command result
     * @return result of the processing
     * @see CommandActionExecutor#processActionCommand(CommandMessage)
     */
    protected <T> CommandMessage<T> localExecutionResult(CommandMessage<T> commandMessage) {
        return lowLevelActionExecutor.processActionCommand(commandMessage);
    }

    // private methods
    // To pass processed message out
    private <T> void passProcessedMessageOut(final CommandMessage<T> processedMessage, final String correlationId) {
        // try to send the result to the responses processor
        if (getResponsesProcessor().accept(processedMessage)) {
            // successfully sent
            getLogger().debug("Result: message with correlationId='{}' is processed and put to responses processor", correlationId);
        } else {
            // something went wrong
            getLogger().error("Result: message with correlationId='{}' is processed but is NOT sent to responses processor", correlationId);
            // simulate successful finalize result message processing
            getResponsesProcessor().onTakenMessage(processedMessage);
        }
    }

    // log that message with correlationId is not found in progress map
    private void logMessageIsNotInProgress(final String correlationId) {
        getLogger().warn("= Message with correlationId='{}' is NOT found in progress map", correlationId);
    }
}
