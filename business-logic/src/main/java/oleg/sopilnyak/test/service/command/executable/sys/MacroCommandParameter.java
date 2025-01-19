package oleg.sopilnyak.test.service.command.executable.sys;

import lombok.ToString;
import lombok.Value;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Type-wrapper: The wrapper of MacroCommand input parameter
 */
@Value
@ToString
public class MacroCommandParameter implements Input<MacroCommandParameter> {
    Input<?> inputParameter;
    Deque<Context<?>> nestedContexts = new LinkedList<>();

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
