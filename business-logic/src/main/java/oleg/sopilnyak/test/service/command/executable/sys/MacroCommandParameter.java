package oleg.sopilnyak.test.service.command.executable.sys;

import lombok.Data;
import lombok.ToString;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Type-wrapper: The wrapper of MacroCommand input parameter
 */
@Data
@ToString
public class MacroCommandParameter implements Input<MacroCommandParameter> {
    private final Input<?> inputParameter;
    private final Deque<Context<?>> nestedContexts = new LinkedList<>();

    public MacroCommandParameter(final Input<?> macroOriginalParameter, final Deque<Context<?>> nestedContexts) {
        this.inputParameter = macroOriginalParameter;
        this.nestedContexts.addAll(nestedContexts);
    }

    /**
     * To get the value of command input-output entity
     *
     * @return value of the parameter
     */
    @Override
    public MacroCommandParameter value() {
        return this;
    }
}
