package oleg.sopilnyak.test.service.command.io.parameter;

import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;

import java.util.Deque;
import java.util.LinkedList;
import java.util.stream.Collectors;


/**
 * Type: I/O school-command contexts deque input parameter
 *
 * @see Input
 * @see Deque
 * @see Context
 */
public class UndoDequeContextsParameter  implements Input<Deque<Context<?>>> {
    // TODO JSON serializer/deserializer should be added
    private final Deque<Context<?>> deque;

    public UndoDequeContextsParameter(Deque<Context<?>> contextDeque) {
        this.deque = contextDeque.stream().collect(Collectors.toCollection(LinkedList::new));
    }

    @Override
    public final boolean equals(Object o) {
        return o instanceof UndoDequeContextsParameter that && deque.equals(that.deque);
    }

    @Override
    public int hashCode() {
        return deque.hashCode();
    }

    @Override
    public Deque<Context<?>> value() {
        return deque;
    }
}
