package oleg.sopilnyak.test.service.command.executable.core.executor.messaging;

import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;

import org.slf4j.Logger;

/**
 * Facade: to execute command using request/response command-messages model
 *
 * @see oleg.sopilnyak.test.service.command.executable.core.executor.messaging.local.LocalQueueCommandExecutor
 * @see oleg.sopilnyak.test.service.command.executable.core.executor.messaging.CommandMessagesExchangeExecutorAdapter
 */
public interface CommandThroughMessagesExecutor extends CommandActionExecutor {
    String REQUEST_MESSAGES_PROCESSOR_NAME = "RequestMessagesProcessor";
    String RESPONSE_MESSAGES_PROCESSOR_NAME = "ResponseMessagesProcessor";
    String JSON_CONTEXT_MODULE_BEAN_NAME = "jsonContextModule";
    String COMMAND_MESSAGE_OBJECT_MAPPER_BEAN_NAME = "commandsTroughMessageObjectMapper";

    /**
     * To start executor with background processors for command-requests to process
     */
    @Override
    default void initialize() {
        throw new UnsupportedOperationException("Please implement it.");
    }

    /**
     * To shut down executor with background processors for command-requests to process
     */
    @Override
    default void shutdown() {
        throw new UnsupportedOperationException("Please implement it.");
    }

    /**
     * To get the logger of the executor implementation
     *
     * @return logger instance
     */
    Logger getLogger();
}
