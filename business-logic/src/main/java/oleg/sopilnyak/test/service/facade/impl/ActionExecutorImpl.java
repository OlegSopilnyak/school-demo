package oleg.sopilnyak.test.service.facade.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import org.slf4j.Logger;

@Slf4j
@AllArgsConstructor
public class ActionExecutorImpl implements ActionExecutor {
    private final MessageProcessingService messageProcessingService;
    /**
     * To get the logger of the executor implementation
     *
     * @return logger instance
     */
    @Override
    public Logger getLogger() {
        return log;
    }

    /**
     * To process action command message,
     * using message-processing-service
     *
     * @param message the action command message
     * @return processed command message
     * @see BaseCommandMessage
     */
    @Override
    public <T> BaseCommandMessage<T> processActionCommand(final BaseCommandMessage<T> message) {
        final String correlationId = message.getCorrelationId();
        log.info("Sending command message for processing, correlationId='{}'", correlationId);
        messageProcessingService.send(message);
        log.info("Waiting for processed command message, correlationId='{}'", correlationId);
        return messageProcessingService.receive(correlationId);
    }
}
