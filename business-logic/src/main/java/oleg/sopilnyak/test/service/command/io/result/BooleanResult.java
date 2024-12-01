package oleg.sopilnyak.test.service.command.io.result;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import oleg.sopilnyak.test.service.command.io.Output;

/**
 * Type: I/O school-command Boolean command execution result
 *
 * @see Output
 */
@Data
@Builder
public class BooleanResult implements Output<Boolean> {
    @Getter(AccessLevel.NONE)
    private final Boolean value;
    /**
     * To get the value of command execution result
     *
     * @return value of the result
     */
    @Override
    public Boolean value() {
        return value;
    }
}
