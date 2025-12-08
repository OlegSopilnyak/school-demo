package oleg.sopilnyak.test.service.facade.impl;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandMessage;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
     * To process processing command message,
     * using message-processing-service
     *
     * @param commandMessage the processing command message
     * @return processed command message
     * @see BaseCommandMessage
     */
    @Override
    public <T> CommandMessage<T> processActionCommand(final CommandMessage<T> commandMessage) {
        log.debug("Validating message before processing...");
        validate(commandMessage);
        // send and receive message for processing
        final String correlationId = commandMessage.getCorrelationId();
        log.info("Sending command message for processing, correlationId='{}'", correlationId);
        // send message for processing
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
    private <T> void validate(CommandMessage<T> message) {
        if (isNull(message.getDirection())) {
            throw new IllegalArgumentException("Message direction is not defined.");
        }
        if (message.getDirection() != CommandMessage.Direction.DO && message.getDirection() != CommandMessage.Direction.UNDO) {
            log.error("Unknown message direction: '{}' for command '{}'.", message.getDirection(), message.getContext().getCommand().getId());
            throw new IllegalArgumentException("Unknown message direction: " + message.getDirection());
        }
    }
}
