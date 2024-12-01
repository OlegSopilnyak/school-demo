package oleg.sopilnyak.test.service.command.io;

import oleg.sopilnyak.test.service.command.type.base.Context;

import java.io.Serializable;


/**
 * Type: I/O school-command execution result
 *
 * @param <O> the type of command execution result value
 * @see oleg.sopilnyak.test.service.command.type.base.RootCommand#executeDo(Context)
 * @see Context#setRedoParameter(Object)
 */
public interface Output<O> extends Serializable {
    /**
     * To get the value of command execution result
     *
     * @return value of the result
     */
    O value();
}
