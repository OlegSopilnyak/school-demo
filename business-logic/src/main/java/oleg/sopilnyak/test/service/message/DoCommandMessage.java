package oleg.sopilnyak.test.service.message;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.Data;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.Output;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.message.payload.BasePayload;
import oleg.sopilnyak.test.service.message.payload.FacultyPayload;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

/**
 * Message: message to commands subsystem
 *
 * @see BasePayload
 */
@Data
public class DoCommandMessage<I, O> implements Serializable {
    // correlation ID of the message
    private String correlationId;
    // the ID of command to execute
    private String commandId;
    // the command's parameters
    private Input<I> parameter;
    // the result of command's execution
    private Output<O> result;
    // the state after command execution
    private Context.State resultState;
    // the time when execution starts
    private Instant startedAt;
    // the value of command execution duration
    private Duration duration;

    void foo(){
        JavaType type = TypeFactory.defaultInstance().constructParametricType(DoCommandMessage.class, FacultyPayload.class);
    }
}
