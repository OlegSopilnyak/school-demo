package oleg.sopilnyak.test.service.command;

import oleg.sopilnyak.test.service.CommandsFactory;

import java.util.function.Supplier;

import static java.util.Objects.nonNull;

/**
 * Class-utility: School command executor
 */
public final class CommandExecutor {
    private CommandExecutor() {
    }

    /**
     * To execute simple school-command
     *
     * @param commandId command-id
     * @param option    command option
     * @param factory   factory of the commands
     * @param <T>       type of command result
     * @return result of command execution
     */
    public static <T> T executeSimpleCommand(String commandId, Object option, CommandsFactory factory) {
        final SchoolCommand<T> command = takeValidCommand(commandId, factory);
        final CommandResult<T> cmdResult = command.execute(option);
        return cmdResult.isSuccess() ?
                cmdResult.getResult().orElseThrow(throwFor(commandId)) :
                throwFor(commandId, cmdResult.getException());
    }

    /**
     * To valid command from factory or throw RuntimeException
     *
     * @param commandId id of the command to take
     * @param factory commands factory
     * @return valid taken command
     * @param <T> type of command result
     */
    public static <T> SchoolCommand<T> takeValidCommand(String commandId, CommandsFactory factory) {
        final SchoolCommand<T> command = factory.command(commandId);
        return nonNull(command) ? command :
                throwFor(commandId, new Exception("Command '" + commandId + "' is not registered in factory."));
    }

    /**
     * To throw Runtime-exception for the command which thrown exception
     *
     * @param commandId command-id where something went wrong
     * @param e         unhandled exception occurred during command execution
     * @return nothing
     */
    public static <T> T throwFor(String commandId, Exception e) {
        throw new RuntimeException("Cannot execute command '" + commandId + "'", e);
    }

    /**
     * To throw Runtime-exception for command from command execution context
     *
     * @param commandId command-id where something went wrong
     * @return Runtime-exception instance
     * @see CommandExecutor#executeSimpleCommand(String, Object, CommandsFactory)
     */
    public static Supplier<RuntimeException> throwFor(String commandId) {
        return () -> new RuntimeException("Cannot execute command '" + commandId + "'");
    }

}
