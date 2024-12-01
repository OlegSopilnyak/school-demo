package oleg.sopilnyak.test.service.command.io.result;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.service.command.io.Output;

/**
 * Type: I/O school-command Model type command execution result
 *
 * @param <P> type of the result
 * @see BaseType
 * @see Output
 */
@Data
@Builder
public class ModelTypeResult<P extends BaseType> implements Output<P> {
    @Getter(AccessLevel.NONE)
    private final P value;
    /**
     * To get the value of command execution result
     *
     * @return value of the result
     */
    @Override
    public P value() {
        return value;
    }
}
