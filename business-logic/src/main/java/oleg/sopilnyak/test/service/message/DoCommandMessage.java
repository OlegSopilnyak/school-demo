package oleg.sopilnyak.test.service.message;

import lombok.experimental.SuperBuilder;

/**
 * Message: message to commands subsystem (to do doCommand)
 *
 * @param <T> type of command execution result
 * @see BaseCommandMessage
 */
@SuperBuilder
public class DoCommandMessage<T> extends BaseCommandMessage<T> {

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
