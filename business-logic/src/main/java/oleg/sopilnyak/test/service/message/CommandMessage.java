package oleg.sopilnyak.test.service.message;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.Output;
import oleg.sopilnyak.test.service.command.type.base.Context;

public interface CommandMessage<I, O> extends Serializable {
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
     * the direction of command's execution
     *
     * @return type of the command's message
     * @see Direction#DO
     * @see Direction#UNDO
     */
    Direction getDirection();

    /**
     * the ID of command to execute
     *
     * @return the value
     * @see oleg.sopilnyak.test.service.command.factory.base.CommandsFactory
     */
    String getCommandId();

    /**
     * the command's input/undo parameters
     *
     * @return the command's input value
     * @see Input#value()
     */
    Input<I> getParameter();

    /**
     * the result of command's do execution
     *
     * @return command do execution's result value
     * @see Output
     */
    Output<O> getResult();

    /**
     * the error instance when something went wrong
     *
     * @return the value or null if there was no error
     * @see Exception
     * @see Context.State#FAIL
     */
    Exception getError();

    /**
     * the state after command execution
     *
     * @return the value
     * @see Context.State
     */
    Context.State getResultState();

    /**
     * the time when command's execution is started
     *
     * @return the value
     * @see Instant
     */
    Instant getStartedAt();

    /**
     * the value of command execution duration
     *
     * @return the value
     * @see Duration
     */
    Duration getDuration();

    /**
     * Enumeration of command execution direction whether it DO or UNDO of command's execution
     */
    enum Direction {
        DO, UNDO
    }
}

