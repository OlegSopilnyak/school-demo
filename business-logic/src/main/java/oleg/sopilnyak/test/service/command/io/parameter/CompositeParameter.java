package oleg.sopilnyak.test.service.command.io.parameter;

import oleg.sopilnyak.test.service.command.io.CompositeInput;
import oleg.sopilnyak.test.service.command.io.Input;

import java.util.Collection;
import org.springframework.util.ObjectUtils;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Type: I/O school-command the composite array of inputs parameter
 *
 * @see Input
 */
@JsonSerialize(using = ParametersContainer.Serializer.class)
@JsonDeserialize(using = ParametersContainer.Deserializer.class)
public final class CompositeParameter<T> extends ParametersContainer<T> implements CompositeInput<T> {
    public CompositeParameter(Input<?>... inputs) {
        super(inputs);
    }

    public CompositeParameter(Collection<T> inputs) {
        super(inputs);
    }

    /**
     * To get the value of command input-output entity
     *
     * @return value of the parameter
     */
    @Override
    public Input<T>[] value() {
        return nest;
    }

    /**
     * To check is result's output value is empty
     *
     * @return true if no data in the output result
     */
    @Override
    public boolean isEmpty() {
        return ObjectUtils.isEmpty(nest);
    }
}
