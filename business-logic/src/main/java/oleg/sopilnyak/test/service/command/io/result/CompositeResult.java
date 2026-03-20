package oleg.sopilnyak.test.service.command.io.result;

import oleg.sopilnyak.test.service.command.io.CompositeOutput;
import oleg.sopilnyak.test.service.command.io.Output;

import java.util.Collection;
import org.springframework.util.ObjectUtils;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Type: I/O school-command the composite array of output results
 *
 * @see Output
 */
@JsonSerialize(using = ResultsContainer.Serializer.class)
@JsonDeserialize(using = ResultsContainer.Deserializer.class)
public class CompositeResult<T> extends ResultsContainer<T> implements CompositeOutput<T> {
    public CompositeResult(Output<?>... outputs) {
        super(outputs);
    }

    public CompositeResult(Collection<T> outputs) {
        super(outputs);
    }

    /**
     * To get the value of command's output-result entity
     *
     * @return the value of the result
     */
    @Override
    public Output<T>[] value() {
        return this.nest;
    }

    /**
     * To check is result's output value is empty
     *
     * @return true if no data in the results container
     */
    @Override
    public boolean isEmpty() {
        return ObjectUtils.isEmpty(nest);
    }
}
