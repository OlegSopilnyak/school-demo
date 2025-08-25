package oleg.sopilnyak.test.service.facade.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;
import org.slf4j.Logger;

@Slf4j
@AllArgsConstructor
public class ActionExecutorImpl implements ActionExecutor {
    private final CommandThroughMessageService messagesExchangeService;
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
     * @param commandMessage the action command message
     * @return processed command message
     * @see BaseCommandMessage
     */
    @Override
    public <T> BaseCommandMessage<T> processActionCommand(final BaseCommandMessage<T> commandMessage) {
        final String correlationId = commandMessage.getCorrelationId();
        log.info("Sending command message for processing, correlationId='{}'", correlationId);
        messagesExchangeService.send(commandMessage);
        log.info("Waiting for processed command message, correlationId='{}'", correlationId);
        return messagesExchangeService.receive(correlationId);
    }
}
