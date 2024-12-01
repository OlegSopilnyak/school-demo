package oleg.sopilnyak.test.service.command.io.result;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.service.command.io.Output;

import java.util.Set;

/**
 * Type: I/O school-command set of Model-types command execution result
 *
 * @param <P> type of the result's set elements
 * @see Set
 * @see BaseType
 * @see Output
 */
@Data
@Builder
public class ModelSetResult<P extends BaseType> implements Output<Set<P>> {
    @Getter(AccessLevel.NONE)
    private final Set<P> value;
    /**
     * To get the value of command execution result
     *
     * @return value of the result
     */
    @Override
    public Set<P> value() {
        return value;
    }
}
