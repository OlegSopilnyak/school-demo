package oleg.sopilnyak.test.service.message;

import lombok.Builder;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.type.base.Context;

/**
 * Message: message to commands subsystem (to execute doCommand)
 *
 * @param <T> type of command execution result
 * @see BaseCommandMessage
 * @see oleg.sopilnyak.test.service.command.type.base.RootCommand#doCommand(Context)
 */
public class DoCommandMessage<T> extends BaseCommandMessage<T> {
    /**
     * Factory method to create message to commands subsystem (to execute doCommand)
     *
     * @param correlationId correlation id of the message
     * @param actionContext action context of the message
     * @param context       context of the command execution
     * @param <T>           type of command execution result
     * @return instance of DoCommandMessage
     */
    @Builder
    private static <T> DoCommandMessage<T> of(String correlationId, ActionContext actionContext, Context<T> context) {
        final DoCommandMessage<T> message = new DoCommandMessage<>(correlationId, actionContext, context);
        message.validate();
        return message;
    }

    /**
     * Private constructor to create message to commands subsystem (to execute doCommand)
     *
     * @param correlationId correlation id of the message
     * @param actionContext action context of the message
     * @param context       context of the command execution
     */
    private DoCommandMessage(String correlationId, ActionContext actionContext, Context<T> context) {
        super(correlationId, actionContext, context);
    }

    /**
     * the direction of command's execution
     *
     * @return type of the command's message
     * @see oleg.sopilnyak.test.service.message.CommandMessage.Direction#DO
     */
    @Override
    public Direction getDirection() {
        return Direction.DO;
    }
}
