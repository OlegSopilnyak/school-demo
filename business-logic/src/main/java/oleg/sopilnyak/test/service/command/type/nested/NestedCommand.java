package oleg.sopilnyak.test.service.command.type.nested;

import java.io.Serializable;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;

/**
 * Type: Command to execute the business-logic action as a nested-command of CompositeCommand
 *
 * @param <T> the type of command execution (do) result
 * @see CompositeCommand
 */
public interface NestedCommand<T> extends Serializable {
    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareNestedContextVisitor#prepareContext(NestedCommand, Input)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Input)
     */
    Context<T> acceptPreparedContext(PrepareNestedContextVisitor visitor, Input<?> macroInputParameter);

    /**
     * To create failed context for the nested-command
     *
     * @param cause cause of fail
     * @return instance of failed command-context
     */
    Context<T> createFailedContext(Exception cause);
}
