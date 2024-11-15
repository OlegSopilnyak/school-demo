package oleg.sopilnyak.test.service.message;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import lombok.Data;
import oleg.sopilnyak.test.service.message.payload.BasePayload;
import oleg.sopilnyak.test.service.message.payload.FacultyPayload;

import java.io.Serializable;

/**
 * Message: message to commands subsystem
 *
 * @see BasePayload
 */
@Data
public class DoCommandMessage<T extends BasePayload<?>> implements Serializable {
    // correlation ID of the message
    private String correlationId;
    // the ID of command to execute
    private String commandId;
    // The command's parameters
    private Object parameters;
    // The result of command's execution
    private Object result;

    void foo(){
        JavaType type = TypeFactory.defaultInstance().constructParametricType(DoCommandMessage.class, FacultyPayload.class);
    }
}
