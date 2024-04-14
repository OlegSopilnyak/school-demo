package oleg.sopilnyak.test.service.command.executable.sys;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import oleg.sopilnyak.test.service.command.type.base.Context;

import java.util.Deque;

/**
 * Type-wrapper: The wrapper of MacroCommand input parameter
 */
@Data
@Builder
@ToString
public class CommandParameterWrapper {
    private Object input;
    private Deque<Context<?>> nestedContexts;
}
