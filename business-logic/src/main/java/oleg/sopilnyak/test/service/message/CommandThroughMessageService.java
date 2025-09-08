package oleg.sopilnyak.test.service.message;

import oleg.sopilnyak.test.service.facade.impl.ActionExecutorImpl;

/**
 * Service: execute command using request/response model
 *
 * @see ActionExecutorImpl
 */
public interface CommandThroughMessageService {
    /**
     * To send command message for processing
     *
     * @param message the command message to be sent
     * @param <T> the type of command execution result in command message
     * @see BaseCommandMessage
     */
    <T> void send(BaseCommandMessage<T> message);

    /**
     * To process the request message's action command in new transaction
     * and send the response to responses queue
     *
     * @param requestMessage message to process
     */
    void processActionCommandAndProceed(final BaseCommandMessage<?> requestMessage);
    /**
     * To receive processed command message (sent by send(message)) by correlationId
     *
     * @param <T>           the type of command execution result in command message
     * @param commandId     the command id of the command in the message
     * @param correlationId the correlation id to find processed command message
     * @return the processed command message
     * @see BaseCommandMessage
     */
    <T> BaseCommandMessage<T> receive(String commandId, String correlationId);
}
