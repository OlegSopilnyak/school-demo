package oleg.sopilnyak.test.service.command.io;

import java.io.Serializable;

/**
 * Type: I/O school-command input-output base type
 *
 * @param <P> the type of command input-output entity
 */
public interface IOBase<P> extends IOFieldNames, Serializable {
    /**
     * To get the value of command input-output entity
     *
     * @return value of the parameter
     */
    P value();
}
