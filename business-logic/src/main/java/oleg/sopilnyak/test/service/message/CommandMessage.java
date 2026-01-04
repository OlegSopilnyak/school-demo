package oleg.sopilnyak.test.service.message;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.type.core.Context;

import java.io.Serializable;

public interface CommandMessage<T> extends Serializable {
    CommandMessage<?> EMPTY = new BaseCommandMessage<>(null, null, null) {
        @Override
        public Direction getDirection() {
            return null;
        }
    };

    /**
     * correlation ID of the message
     *
     * @return the value
     */
    String getCorrelationId();

    /**
     * the processing context of command's execution
     *
     * @return the value
     * @see ActionContext#getFacadeName()
     * @see ActionContext#getActionName()
     */
    ActionContext getActionContext();

    /**
     * the context of command's execution
     *
     * @return the value
     * @see Context
     */
    Context<T> getContext();

    /**
     * the direction of command's execution
     *
     * @return type of the command's message
     * @see Direction#DO
     * @see Direction#UNDO
     */
    Direction getDirection();

    /**
     * Enumeration of command execution direction whether it DO or UNDO of command's execution
     */
    enum Direction {
        DO, UNDO, UNKNOWN
    }
}

