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
public class MacroCommandParameter<T> {
    private Object input;
    private Deque<Context<T>> nestedContexts = new LinkedList<>();

    public MacroCommandParameter(Object input, Deque<Context<T>> nestedContexts) {
        this.input = input;
        this.nestedContexts.addAll(nestedContexts);
    }
}
