package oleg.sopilnyak.test.service.message;

/**
 * Message: message to commands subsystem (to do undoCommand)
 *
 * @param <I> type of input parameter
 * @param <O> type of output result
 * @see BaseCommandMessage
 */
public class UndoCommandMessage<I, O> extends BaseCommandMessage<I, O> {
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
