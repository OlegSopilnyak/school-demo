package oleg.sopilnyak.test.service.command.io;

import java.io.Serializable;

import static java.util.Objects.isNull;

/**
 * Type: I/O school-command input-output base type
 *
 * @param <P> the type of command input-output entity
 */
public interface IOBase<P> extends Serializable {
    /**
     * To get the value of command input-output entity
     *
     * @return value of the parameter
     */
    P value();
    /**
     * To check is result's output value is empty
     *
     * @return true if no data in the output result
     */
    default boolean isEmpty() {
        return isNull(value());
    }
}
