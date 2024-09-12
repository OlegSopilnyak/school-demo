package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;

/**
 * Wrapper for chained nested commands in sequence
 * @see SequentialMacroCommand
 */
public interface ChainedNestedCommand<T extends RootCommand> extends NestedCommand.InSequence {
    /**
     * To unwrap nested command
     *
     * @return unwrapped instance of the command
     */
    T unWrap();
}

