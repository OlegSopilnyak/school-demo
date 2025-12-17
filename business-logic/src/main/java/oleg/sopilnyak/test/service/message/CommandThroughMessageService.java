package oleg.sopilnyak.test.service.message;

import oleg.sopilnyak.test.service.facade.impl.ActionExecutorImpl;

/**
 * Service: execute command using request/response model
 *
 * @see ActionExecutorImpl
 */
public interface CommandThroughMessageService {
    String JSON_CONTEXT_MODULE_BEAN_NAME = "jsonContextModule";
    String COMMAND_MESSAGE_OBJECT_MAPPER_BEAN_NAME = "commandsTroughMessageObjectMapper";
    /**
     * To start background processors for requests to process
     */
    void initialize();

    /**
     * To shut down background processors for requests to process
     */
    void shutdown();

    /**
     * To send command message for processing
     *
     * @param message the command message to be sent
     * @param <T>     the type of command execution result in command message
     * @see CommandMessage
     */
    <T> void send(CommandMessage<T> message);

    /**
     * To receive processed command message (sent by send(message)) by correlationId
     *
     * @param <T>           the type of command execution result in command message
     * @param commandId     the command id of the command in the message
     * @param correlationId the correlation id to find processed command message
     * @return the processed command message
     * @see BaseCommandMessage
     */
    <T> CommandMessage<T> receive(String commandId, String correlationId);
}
