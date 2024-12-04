package oleg.sopilnyak.test.service.command.io;

import oleg.sopilnyak.test.service.command.type.base.Context;

import static java.util.Objects.isNull;


/**
 * Type: I/O school-command execution result
 *
 * @param <O> the type of command execution result value
 * @see oleg.sopilnyak.test.service.command.type.base.RootCommand#executeDo(Context)
 * @see Context#setResult(Object)
 */
public interface Output<O>  extends IOBase<O> {
    /**
     * To check is result's output value is empty
     *
     * @return true if no data in the output result
     */
    default boolean isEmpty() {
        return isNull(value());
    }
}
