package oleg.sopilnyak.test.service.command;


import oleg.sopilnyak.test.service.CommandsFactory;
import oleg.sopilnyak.test.service.exception.CommandNotRegisteredInFactoryException;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;

import java.util.function.Supplier;

import static java.util.Objects.nonNull;

/**
 * The main engine for execute school commands
 */
public interface CommandExecutor {
    /**
     * To execute simple school-command
     *
     * @param commandId command-id
     * @param option    command option
     * @param factory   factory of the commands
     * @param <T>       type of command result
     * @return result of command execution
     * @see CommandsFactory
     * @see SchoolCommand
     */
    static <T> T executeSimpleCommand(String commandId, Object option, CommandsFactory factory) {
        return executeCommand(takeValidCommand(commandId, factory), option);
    }

    /**
     * To execute simple command with parameters
     *
     * @param command command to execute
     * @param option  command's parameter(s)
     * @param <T>     type of command result
     * @return result of command execution
     * @see SchoolCommand#execute(Object)
     * @see CommandResult
     * @see CommandResult#isSuccess()
     * @see CommandResult#getResult()
     * @see CommandResult#getException()
     * @see CommandExecutor#throwFor(String, Exception)
     * @see CommandExecutor#createThrowFor(String)
     */
    static <T> T executeCommand(SchoolCommand<T> command, Object option) {
        final CommandResult<T> cmdResult = command.execute(option);
        return !cmdResult.isSuccess() ?
                throwFor(command.getId(), cmdResult.getException()) :
                cmdResult.getResult().orElseThrow(createThrowFor(command.getId()));
    }

    /**
     * To take valid command from factory or throw UnableExecuteCommandException
     *
     * @param commandId id of the command to take
     * @param factory   commands factory
     * @param <T>       type of command result
     * @return valid taken command or throws
     * @see CommandsFactory
     * @see CommandsFactory#command(String)
     * @see CommandExecutor#throwFor(String, Exception)
     * @see CommandNotRegisteredInFactoryException
     */
    static <T> SchoolCommand<T> takeValidCommand(final String commandId, final CommandsFactory factory) {
        final SchoolCommand<T> command = factory.command(commandId);
        return nonNull(command) ? command :
                throwFor(commandId, new CommandNotRegisteredInFactoryException(commandId, factory));
    }

    /**
     * To create supplier to throw Runtime-exception for command from command execution context
     *
     * @param commandId command-id where something went wrong
     * @return Runtime-exception instance
     * @see CommandExecutor#executeSimpleCommand(String, Object, CommandsFactory)
     */
    static Supplier<RuntimeException> createThrowFor(final String commandId) {
        return () -> new UnableExecuteCommandException(commandId);
    }


    /**
     * To throw UnableExecuteCommandException for the command which thrown exception
     *
     * @param commandId command-id where something went wrong
     * @param e         unhandled exception occurred during command execution
     * @return nothing
     */
    static <T> T throwFor(final String commandId, final Exception e) {
        throw new UnableExecuteCommandException(commandId, e);
    }
}
