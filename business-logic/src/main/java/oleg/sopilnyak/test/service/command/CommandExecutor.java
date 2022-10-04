package oleg.sopilnyak.test.service.command;

import oleg.sopilnyak.test.service.CommandsFactory;

import java.util.function.Supplier;

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
        SchoolCommand<T> command = factory.command(commandId);
        CommandResult<T> cmdResult = command.execute(option);
        if (cmdResult.isSuccess()) {
            return cmdResult.getResult().orElseThrow(throwFor(commandId));
        }
        return throwFor(commandId, cmdResult.getException());
    }

    /**
     * To throw Runtime-exception for command
     *
     * @param commandId command-id where something went wrong
     * @return Runtime-exception instance
     */
    public static Supplier<RuntimeException> throwFor(String commandId) {
        return () -> new RuntimeException("Cannot execute command '" + commandId + "'");
    }

    /**
     * To throw Runtime-exception for command
     *
     * @param commandId command-id where something went wrong
     * @param e unhandled exception occurred during command execution
     * @return nothing
     */
    public static<T> T throwFor(String commandId, Exception e) {
        throw  new RuntimeException("Cannot execute command '" + commandId + "'", e);
    }
}
