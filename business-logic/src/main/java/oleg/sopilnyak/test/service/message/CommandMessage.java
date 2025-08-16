package oleg.sopilnyak.test.service.message;

import java.io.Serializable;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.type.base.Context;

public interface CommandMessage<T> extends Serializable {
    /**
     * correlation ID of the message
     *
     * @return the value
     */
    String getCorrelationId();

    /**
     * the action context of command's execution
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
        DO, UNDO
    }
}

