package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.executable.sys.CommandParameterWrapper;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.stream.Collectors;

/**
 * Type: Command to execute the couple of commands
 */
public interface CompositeCommand extends SchoolCommand {

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
    Collection<SchoolCommand> commands();

    /**
     * To add the command
     *
     * @param command the instance to add
     */
    void add(SchoolCommand command);

    /**
     * To create command's context with doParameter
     *
     * @param input context's doParameter value
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
                        .<Context<T>>map(command -> this.prepareContext(command, input))
                        .collect(Collectors.toCollection(LinkedList::new))
        );
        // assemble input parameter contexts for redo
        final Context<T> context = createContext();
        context.setRedoParameter(doParameter);
        return context;
    }

    /**
     * To prepare context for particular command
     *
     * @param nestedCommand     nested command instance
     * @param macroCommandInput macro-command input parameter
     * @return built context of the command for input parameter
     * @see SchoolCommand
     * @see SchoolCommand#createContext(Object)
     * @see Context
     */
    default <T> Context<T> prepareContext(SchoolCommand nestedCommand, Object macroCommandInput) {
        return nestedCommand.createContext(macroCommandInput);
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
}
