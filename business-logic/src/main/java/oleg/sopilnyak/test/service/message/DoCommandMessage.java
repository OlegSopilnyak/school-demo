package oleg.sopilnyak.test.service.message;

/**
 * Message: message to commands subsystem (to do doCommand)
 *
 * @param <I> type of input parameter
 * @param <O> type of output result
 * @see BaseCommandMessage
 */
public class DoCommandMessage<I, O> extends BaseCommandMessage<I, O> {
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
