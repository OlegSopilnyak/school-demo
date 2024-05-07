package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.executable.sys.CommandParameterWrapper;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Deque;
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
        final Context<T> context = createContext();
        final Deque<Context<?>> nested = commands().stream()
                .map(command -> this.prepareContext(command, input))
                .collect(Collectors.toCollection(LinkedList::new));
        // assemble input parameter contexts for redo
        context.setRedoParameter(CommandParameterWrapper.builder().input(input).nestedContexts(nested).build());
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
     * @param context context of redo execution
     * @see Context
     */
    @Override
    default void doCommand(Context<?> context) {
        if (isWrongRedoStateOf(context)) {
            getLog().warn("Cannot do command '{}' with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        } else {
            // start do execution with correct context state
            context.setState(Context.State.WORK);
            executeDo(context);
        }
    }

    /**
     * To rollback command's execution
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    @Override
    default void undoCommand(Context<?> context) {
        if (isWrongUndoStateOf(context)) {
            getLog().warn("Cannot do undo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        } else {
            // start undo with correct context state
            context.setState(Context.State.WORK);
            executeUndo(context);
        }
    }
}
