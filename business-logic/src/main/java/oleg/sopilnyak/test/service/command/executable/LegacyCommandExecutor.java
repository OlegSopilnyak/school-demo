package oleg.sopilnyak.test.service.command.executable;


import static java.util.Objects.nonNull;

import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.exception.CommandNotRegisteredInFactoryException;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The main engine for execute school commands
 * @deprecated
 */
@Deprecated(since = "CommandActionExecutor is used instead")
public interface LegacyCommandExecutor {
    /**
     * To do simple school-command using Context
     *
     * @param commandId command-id
     * @param input     command input parameter
     * @param factory   factory of the commands
     * @param <T>       type of command result
     * @param <C>       type of command to execute doCommand method
     * @return result of command execution
     * @see CommandsFactory
     * @see RootCommand
     * @see LegacyCommandExecutor#takeValidCommand(String, CommandsFactory)
     * @see LegacyCommandExecutor#doCommand(RootCommand, Input)
     */
    static <T, C extends RootCommand<?>> T doSimpleCommand(final String commandId,
                                                           final Input<?> input,
                                                           final CommandsFactory<C> factory) {
        final C command = takeValidCommand(commandId, factory);
        return doCommand(command, input);
    }


    /**
     * To do simple command with parameter(s) using Context
     *
     * @param command command to do
     * @param input   command's parameter(s)
     * @param <T>     type of command result
     * @param <C>     type of command to execute doCommand method
     * @return result of command execution
     * @see RootCommand#createContext(Input)
     * @see RootCommand#doCommand(Context)
     * @see Context
     * @see Context#getResult()
     * @see Context#getState()
     * @see LegacyCommandExecutor#throwFor(String, Exception)
     * @see LegacyCommandExecutor#createThrowFor(String)
     */
    private static <T, C extends RootCommand> T doCommand(final C command, final Input<?> input) {
        final Context<T> context = command.createContext(input);
        // doing command's do
        command.doCommand(context);
        // getting command's do result
        final Optional<T> result = context.getResult();
        // returning result
        return context.isDone() ?
                result.orElseThrow(createThrowFor(command.getId())) :
                throwFor(command.getId(), context.getException());
    }


    /**
     * To take valid command from factory or throw UnableExecuteCommandException
     *
     * @param commandId id of the command to take
     * @param factory   commands factory
     * @param <T>       type of command to execute doCommand method
     * @return valid taken command or throws
     * @see CommandsFactory#command(String)
     * @see LegacyCommandExecutor#throwFor(String, Exception)
     * @see CommandNotRegisteredInFactoryException
     */
    static <T extends RootCommand<?>> T takeValidCommand(final String commandId,
                                                         final CommandsFactory<T> factory) {
        final T concreteCommand = factory.command(commandId);
        return nonNull(concreteCommand) ? concreteCommand :
                throwFor(commandId, new CommandNotRegisteredInFactoryException(commandId, factory));
    }

    /**
     * To create supplier to throw Runtime-exception for command from command execution context
     *
     * @param commandId command-id where something went wrong
     * @return Runtime-exception instance
     * @see LegacyCommandExecutor#doCommand(RootCommand, Input)
     */
    static Supplier<RuntimeException> createThrowFor(final String commandId) {
        return () -> new UnableExecuteCommandException(commandId);
    }

    /**
     * To throw UnableExecuteCommandException for the command which thrown exception
     *
     * @param commandId command-id where something went wrong
     * @param e         unhandled exception occurred during command execution
     * @param <T>       type of command result or command type to execute doCommand
     * @return nothing
     * @see LegacyCommandExecutor#doCommand(RootCommand, Input)
     */
    static <T> T throwFor(final String commandId, final Exception e) {
        throw new UnableExecuteCommandException(commandId, e);
    }
}
