package oleg.sopilnyak.test.service.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.Output;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.message.payload.BasePayload;

import java.time.Duration;
import java.time.Instant;

/**
 * Message: message to commands subsystem (to do command)
 *
 * @param <I> type of input parameter
 * @param <O> type of output result
 * @see BasePayload
 */
@Data
public class DoCommandMessage<I, O> implements CommandMessage<I, O> {
    // correlation ID of the message
    private String correlationId;
    // the ID of command to execute
    private String commandId;
    // the context of command's execution
    private ActionContext actionContext;
    // the command's parameters
    private Input<I> parameter;
    // the result of command's execution
    private Output<O> result;
    // The error instance when something went wrong
    @JsonSerialize(using = CommandMessage.ExceptionSerializer.class)
    @JsonDeserialize(using = CommandMessage.ExceptionDeserializer.class)
    private Exception error;
    // the state after command execution
    private Context.State resultState;
    // the time when execution starts
    private Instant startedAt;
    // the value of command execution duration
    private Duration duration;

    /**
     * the direction of command's execution
     *
     * @return type of the command's message
     * @see oleg.sopilnyak.test.service.message.CommandMessage.Direction#DO
     */
    @JsonIgnore
    @Override
    public Direction getDirection() {
        return Direction.DO;
    }
}
