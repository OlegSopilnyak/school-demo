package oleg.sopilnyak.test.service.command.executable.sys;

import lombok.Data;
import lombok.Getter;
import lombok.ToString;
import oleg.sopilnyak.test.service.command.type.base.Context;

import java.util.Deque;
import java.util.LinkedList;

/**
 * Type-wrapper: The wrapper of MacroCommand input parameter
 */
@Data
@Getter
@ToString
public class CommandParameterWrapper<T> {
    private Object input;
    private final LinkedList<Context<T>> nestedContexts = new LinkedList<>();

    public CommandParameterWrapper(Object input, Deque<Context<T>> nestedContexts) {
        this.input = input;
        this.nestedContexts.addAll(nestedContexts);
    }
}
