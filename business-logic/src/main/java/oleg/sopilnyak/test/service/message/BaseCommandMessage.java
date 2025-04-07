package oleg.sopilnyak.test.service.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.io.IOBase;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.Output;
import oleg.sopilnyak.test.service.command.type.base.Context;

import java.time.Duration;
import java.time.Instant;


/**
 * Message: message to commands subsystem (parent of any type of messages)
 *
 * @param <I> type of input parameter
 * @param <O> type of output result
 * @see DoCommandMessage
 */
@Data
public abstract class BaseCommandMessage<I, O> implements CommandMessage<I, O> {
    // correlation ID of the message
    private String correlationId;
    // the ID of command to execute
    private String commandId;
    // the context of command's execution
    @JsonDeserialize(using = IOBase.ActionContextDeserializer.class)
    private ActionContext actionContext;
    // the command's parameters
    @JsonDeserialize(using = Input.ParameterDeserializer.class)
    private Input<I> parameter;
    // the result of command's execution
    @JsonDeserialize(using = Output.ResultDeserializer.class)
    private Output<O> result;
    // The error instance when something went wrong
    @JsonSerialize(using = IOBase.ExceptionSerializer.class)
    @JsonDeserialize(using = IOBase.ExceptionDeserializer.class)
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
     * @see Direction
     */
    @JsonIgnore
    @Override
    public abstract Direction getDirection();
}
