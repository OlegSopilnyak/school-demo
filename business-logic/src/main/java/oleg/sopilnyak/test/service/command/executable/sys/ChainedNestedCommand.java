package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import org.slf4j.Logger;

/**
 * class-wrapper for chained nested commands in sequence
 * @see SequentialMacroCommand
 */
public abstract class ChainedNestedCommand<C extends RootCommand> implements NestedCommand.InSequence, RootCommand {
    /**
     * To unwrap nested command
     *
     * @return unwrapped instance of the command
     */
    public abstract C unWrap();

    @Override
    public Logger getLog() {
        return unWrap().getLog();
    }

    @Override
    public String getId() {
        return unWrap().getId();
    }

    @Override
    public <T> void doAsNestedCommand(final NestedCommandExecutionVisitor visitor,
                                      final Context<T> context,
                                      final Context.StateChangedListener<T> stateListener) {
        unWrap().doAsNestedCommand(visitor, context, stateListener);
    }

    @Override
    public <T> Context<T> undoAsNestedCommand(final NestedCommandExecutionVisitor visitor,
                                              final Context<T> context) {
        return unWrap().undoAsNestedCommand(visitor, context);
    }
}

