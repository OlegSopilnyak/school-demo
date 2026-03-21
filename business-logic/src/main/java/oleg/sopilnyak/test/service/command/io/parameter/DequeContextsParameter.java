package oleg.sopilnyak.test.service.command.io.parameter;

import oleg.sopilnyak.test.service.command.executable.core.MacroCommand;
import oleg.sopilnyak.test.service.command.io.IOBase;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


/**
 * Type: I/O school-command contexts deque input parameter for undo composite command
 *
 * @see Input
 * @see Deque
 * @see Context
 * @see ParametersContainer
 * @see MacroCommand#rollbackNested(Deque)
 */
@JsonSerialize(using = ParametersContainer.Serializer.class)
@JsonDeserialize(using = ParametersContainer.Deserializer.class)
public class DequeContextsParameter<C extends Context<?>> extends ParametersContainer<C> implements Input<Deque<C>> {
    public DequeContextsParameter(Collection<C> inputs) {
        super(inputs);
    }

    /**
     * To get the value of command input-output entity
     *
     * @return value of the parameter
     */
    @Override
    public Deque<C> value() {
        // keeping the order as in the nest
        return Arrays.stream(nest).map(IOBase::value).collect(Collectors.toCollection(LinkedList::new));
    }
}
