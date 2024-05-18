package oleg.sopilnyak.test.service.command.type;

import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.executable.sys.CommandParameterWrapper;
import oleg.sopilnyak.test.service.command.executable.sys.ParallelMacroCommand;
import oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import oleg.sopilnyak.test.service.command.type.composite.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.composite.TransferResultVisitor;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Type: Command to execute the couple of commands
 */
public interface CompositeCommand<C extends SchoolCommand>
        extends
        SchoolCommand,
        PrepareContextVisitor {

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    Logger getLog();

    /**
     * To get the collection of nested commands used it composite
     *
     * @return collection of included commands
     */
    Collection<C> commands();

    /**
     * To add the command
     *
     * @param command the instance to add
     */
    void add(C command);

    /**
     * To create command's context with doParameter
     *
     * @param input context's doParameter value
     * @param <T>   type of command result
     * @return context instance
     * @see Context
     * @see Context#getRedoParameter()
     * @see CommandContext
     * @see Context.State#READY
     * @see SchoolCommand#createContext()
     */
    @Override
    default <T> Context<T> createContext(Object input) {
        final CommandParameterWrapper<T> doParameter = new CommandParameterWrapper<>(input,
                this.commands().stream()
                        .<Context<T>>map(command -> command.acceptPreparedContext(this, input))
                        .collect(Collectors.toCollection(LinkedList::new))
        );
        // assemble input parameter contexts for redo
        return SchoolCommand.super.createContext(doParameter);
    }

    /**
     * To execute command
     *
     * @param doContext context of redo execution
     * @see Context
     */
    @Override
    default <T> void doCommand(Context<T> doContext) {
        if (isWrongRedoStateOf(doContext)) {
            getLog().warn("Cannot do command '{}' with context:state '{}'", getId(), doContext.getState());
            doContext.setState(Context.State.FAIL);
        } else {
            // start do execution with correct context state
            doContext.setState(Context.State.WORK);
            executeDo(doContext);
        }
    }

    /**
     * To rollback command's execution
     *
     * @param undoContext context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    @Override
    default <T> void undoCommand(Context<T> undoContext) {
        if (isWrongUndoStateOf(undoContext)) {
            getLog().warn("Cannot undo command '{}' with context:state '{}'", getId(), undoContext.getState());
            undoContext.setState(Context.State.FAIL);
        } else {
            // start undo with correct context state
            undoContext.setState(Context.State.WORK);
            executeUndo(undoContext);
        }
    }

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor visitor of prepared contexts
     * @param input   Macro-Command call's input
     * @param <T>     type of command result
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(SequentialMacroCommand, Object)
     * @see PrepareContextVisitor#prepareContext(ParallelMacroCommand, Object)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Object)
     */
    @Override
    default <T> Context<T> acceptPreparedContext(PrepareContextVisitor visitor, Object input) {
        return visitor.prepareContext(this, input);
    }

    /**
     * To transfer command execution result to next command context
     *
     * @param visitor visitor for transfer result
     * @param result  result of command execution
     * @param target  command context for next execution
     * @param <S>     type of current command execution result
     * @param <T>     type of next command execution result
     * @see TransferResultVisitor#transferPreviousExecuteDoResult(CompositeCommand, Optional, Context)
     * @see Context#setRedoParameter(Object)
     */
    @Override
    default <S, T> void transferResultTo(
            final TransferResultVisitor visitor, final Optional<S> result, final Context<T> target
    ) {
        visitor.transferPreviousExecuteDoResult(this, result, target);
    }
}
