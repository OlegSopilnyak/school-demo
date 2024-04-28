package oleg.sopilnyak.test.service.command.executable;


import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import oleg.sopilnyak.test.service.exception.CommandNotRegisteredInFactoryException;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;

import java.util.Optional;
import java.util.function.Supplier;

import static java.util.Objects.nonNull;

/**
 * The main engine for execute school commands
 */
public interface CommandExecutor {
    /**
     * To do simple school-command using Context
     *
     * @param commandId command-id
     * @param input     command input parameter
     * @param factory   factory of the commands
     * @param <T>       type of command result
     * @param <P>       type of commands factory
     * @return result of command execution
     * @see CommandsFactory
     * @see SchoolCommand
     */
    static <T, P> T doSimpleCommand(String commandId, Object input, CommandsFactory<P> factory) {
        final SchoolCommand<T> command = takeValidCommand(commandId, factory);
        return doCommand(command, input);
    }


    /**
     * To do simple command with parameter(s) using Context
     *
     * @param command command to do
     * @param input   command's parameter(s)
     * @param <T>     type of command result
     * @return result of command execution
     * @see SchoolCommand#createContext(Object)
     * @see SchoolCommand#doCommand(Context)
     * @see Context
     * @see Context#getResult()
     * @see Context#getState()
     * @see CommandExecutor#throwFor(String, Exception)
     * @see CommandExecutor#createThrowFor(String)
     */
    private static <T> T doCommand(SchoolCommand<T> command, Object input) {
        final String commandId = command.getId();
        final Context<T> context = command.createContext(input);
        // doing command's do
        command.doCommand(context);
        // getting command's do result
        final Optional<T> result = context.getResult();
        // returning result
        return context.isDone() ?
                result.orElseThrow(createThrowFor(commandId)) :
                throwFor(commandId, context.getException());
    }

    /**
     * To execute simple school-command
     *
     * @param commandId command-id
     * @param option    command option
     * @param factory   factory of the commands
     * @param <T>       type of command result
     * @param <P>       type of commands factory
     * @return result of command execution
     * @see CommandsFactory
     * @see SchoolCommand
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    static <T, P> T executeSimpleCommand(String commandId, Object option, CommandsFactory<P> factory) {
        final SchoolCommand<T> command = takeValidCommand(commandId, factory);
        return executeCommand(command, option);
    }

    /**
     * To execute simple command with parameter(s)
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
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    private static <T> T executeCommand(SchoolCommand<T> command, Object option) {
        final CommandResult<T> cmdResult = command.execute(option);
        return cmdResult.isSuccess() ?
                cmdResult.getResult().orElseThrow(createThrowFor(command.getId())) :
                throwFor(command.getId(), cmdResult.getException());
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
        final SchoolCommand<T> concreteCommand = factory.command(commandId);
        return nonNull(concreteCommand) ? concreteCommand :
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
