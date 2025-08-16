package oleg.sopilnyak.test.service.message;

import lombok.experimental.SuperBuilder;

/**
 * Message: message to commands subsystem (to do undoCommand)
 *
 * @param <T> type of command execution result
 * @see BaseCommandMessage
 */
@SuperBuilder
public class UndoCommandMessage<T> extends BaseCommandMessage<T> {
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
