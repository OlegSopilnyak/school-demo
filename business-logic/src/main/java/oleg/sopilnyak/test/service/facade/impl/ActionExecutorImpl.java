package oleg.sopilnyak.test.service.facade.impl;

import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandMessage;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@Deprecated
public class ActionExecutorImpl implements CommandActionExecutor {
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
     * To process doingMainLoop command message,
     * using message-doingMainLoop-service
     *
     * @param commandMessage the doingMainLoop command message
     * @return processed command message
     * @see BaseCommandMessage
     */
    @Override
    public <T> CommandMessage<T> processActionCommand(final CommandMessage<T> commandMessage) {
        log.debug("Validating input message before doingMainLoop...");
        validateInput(commandMessage);
        // send and receive message for doingMainLoop
        final String correlationId = commandMessage.getCorrelationId();
        log.info("Sending command message for doingMainLoop, correlationId='{}'", correlationId);
        // send message for doingMainLoop
        messagesExchangeService.send(commandMessage);
        // wait and get back
        // receiving processed message
        final String commandId = commandMessage.getContext().getCommand().getId();
        log.info("Waiting for processed command message of command '{}' with correlationId='{}'", commandId, correlationId);
        return messagesExchangeService.receive(commandId, correlationId);
    }

    @PreDestroy
    public void shutdownMessagesExchangeService() {
        log.info("Shutting down Messages Exchange Service");
        messagesExchangeService.shutdown();
    }

    // private methods
}
