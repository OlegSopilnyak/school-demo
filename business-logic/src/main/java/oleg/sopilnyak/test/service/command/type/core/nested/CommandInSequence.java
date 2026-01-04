package oleg.sopilnyak.test.service.command.type.core.nested;

import oleg.sopilnyak.test.service.command.executable.core.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;

/**
 * Command Marker: Mark the nested command which is using in the commands sequence
 *
 * @see NestedCommand
 * @see oleg.sopilnyak.test.service.command.executable.core.SequentialMacroCommand
 * @deprecated
 */
@Deprecated
public interface CommandInSequence {
    /**
     * To transfer nested command execution result to target nested command context input
     *
     * @param visitor visitor for do transferring result from source to target
     * @param value   result of source command execution
     * @param target  nested command context for the next execution in sequence
     * @param <S>     type of source command execution result
     * @param <N>     type of target command execution result
     * @see TransferTransitionalResultVisitor#transferPreviousExecuteDoResult(RootCommand, Object, Context)
     * @see CommandContext#setRedoParameter(Input)
     */
    <S, N> void transferResultTo(TransferTransitionalResultVisitor visitor, S value, Context<N> target);
}
