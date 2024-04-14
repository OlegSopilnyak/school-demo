package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.executable.sys.CommandParameterWrapper;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Type: Command to execute the couple of commands
 *
 * @param <T> type of macro-command
 */
public interface CompositeCommand<T> extends SchoolCommand<T> {

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    Logger getLog();

    /**
     * To get the collection of commands used it composite
     *
     * @return collection of included commands
     */
    Collection<SchoolCommand<?>> commands();

    /**
     * To add the command
     *
     * @param command the instance to add
     */
    void add(SchoolCommand<?> command);

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
    default Context<T> createContext(Object input) {
        final Context<T> context = createContext();
        // assemble input parameter for redo
        context.setRedoParameter(CommandParameterWrapper.builder()
                .input(input)
                .nestedContexts(commands().stream()
                        .map(cmd -> prepareContext(cmd, input))
                        .collect(Collectors.toCollection(LinkedList::new)))
                .build());
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
    default Context<?> prepareContext(SchoolCommand<?> nestedCommand, Object macroCommandInput) {
        return nestedCommand.createContext(macroCommandInput);
    }

    /**
     * To execute command's business-logic
     *
     * @param parameter command's parameter
     * @return execution's result
     * @see this#doCommand(Context)
     * @see this#undoCommand(Context)
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    default CommandResult<T> execute(Object parameter) {
        return CommandResult.<T>builder().success(false).result(Optional.empty()).build();
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
            getLog().warn("Cannot do redo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        } else {
            // start redo with correct context state
            context.setState(Context.State.WORK);
            executeDo(context);
        }
    }

    /**
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context.State#WORK
     */
    default void executeDo(Context<?> context) {
        context.setState(Context.State.DONE);
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

    /**
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    default void executeUndo(Context<?> context) {
        context.setState(Context.State.UNDONE);
    }
}
