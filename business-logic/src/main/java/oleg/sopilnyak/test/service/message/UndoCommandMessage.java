package oleg.sopilnyak.test.service.message;

import lombok.Builder;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.type.base.Context;

/**
 * Message: message to commands subsystem (to execute undoCommand)<BR/>
 * type of execution result is not important, because undoCommand does not return any result
 *
 * @see BaseCommandMessage
 * @see oleg.sopilnyak.test.service.command.type.base.RootCommand#undoCommand(Context)
 */
public class UndoCommandMessage extends BaseCommandMessage {
    /**
     * Factory method to create message to commands subsystem (to execute undoCommand)
     * type of execution result is not important, because undoCommand does not return any result
     *
     * @param correlationId correlation id of the message
     * @param actionContext action context of the message
     * @param context       context of the command execution
     * @return instance of DoCommandMessage
     * @see Builder
     * @see BaseCommandMessage#validate()
     */
    @Builder
    private static UndoCommandMessage of(String correlationId, ActionContext actionContext, Context<?> context) {
        final UndoCommandMessage message = new UndoCommandMessage(correlationId, actionContext, context);
        message.validate();
        return message;
    }

    /**
     * Private constructor to create message to commands subsystem (to execute undoCommand)
     *
     * @param correlationId correlation id of the message
     * @param actionContext action context of the message
     * @param context       context of the command execution
     */
    private UndoCommandMessage(String correlationId, ActionContext actionContext, Context<?> context) {
        super(correlationId, actionContext, context);
    }

    /**
     * the direction of command's execution
     *
     * @return type of the command's message
     * @see Direction#UNDO
     */
    @Override
    public Direction getDirection() {
        return Direction.UNDO;
    }
}
