package oleg.sopilnyak.test.service.command.io;

import oleg.sopilnyak.test.service.command.type.base.Context;

import java.io.Serializable;

/**
 * Type: I/O school-command input parameter
 *
 * @param <P> the type of command input parameter
 * @see oleg.sopilnyak.test.service.command.type.base.RootCommand#executeDo(Context)
 * @see Context#setRedoParameter(Object)
 */
public interface Input<P> extends Serializable {
    String VALUE_FIELD_NAME = "value";
    String TYPE_FIELD_NAME = "type";
    String NESTED_TYPE_FIELD_NAME = "nested-type";
    /**
     * To get the value of command input parameter
     *
     * @return value of the parameter
     */
    P value();
}
