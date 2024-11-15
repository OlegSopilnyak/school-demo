package oleg.sopilnyak.test.service.command.executable.sys;

import lombok.Data;
import lombok.ToString;
import oleg.sopilnyak.test.service.command.type.base.Context;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Type-wrapper: The wrapper of MacroCommand input parameter
 */
@Data
@ToString
public class MacroCommandParameter {
    private final Object input;
    private final Deque<Context<?>> nestedContexts = new LinkedList<>();

    public MacroCommandParameter(final Object macroOriginalParameter, final Deque<Context<?>> nestedContexts) {
        this.input = macroOriginalParameter;
        this.nestedContexts.addAll(nestedContexts);
    }
}
