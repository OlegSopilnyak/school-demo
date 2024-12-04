package oleg.sopilnyak.test.service.command.io;

import oleg.sopilnyak.test.service.command.type.base.Context;

/**
 * Type: I/O school-command input parameter
 *
 * @param <P> the type of command input parameter
 * @see oleg.sopilnyak.test.service.command.type.base.RootCommand#executeDo(Context)
 * @see Context#setRedoParameter(Object)
 */
public interface Input<P> extends IOBase<P> {
}
