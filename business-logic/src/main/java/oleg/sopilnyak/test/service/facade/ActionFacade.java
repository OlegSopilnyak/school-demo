package oleg.sopilnyak.test.service.facade;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.function.Consumer;
import java.util.function.Supplier;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.exception.CommandNotRegisteredInFactoryException;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import org.slf4j.Logger;

/**
 * Facade Utility: Facade for manage execution of school actions activity
 *
 * @see ActionContext
 * @see Context
 */
public interface ActionFacade {
    /**
     * To get the logger of the facade
     *
     * @return logger instance
     */
    Logger getLogger();

    /**
     * To get the actions executor instance
     *
     * @return action executor instance
     * @see ActionExecutor
     */
    ActionExecutor getActionExecutor();

    /**
     * To act action command with given command ID, command factory, and input parameter.
     *
     * @param commandId the command id
     * @param factory   the commands factory to find command by id
     * @param input     the input parameter for the command execution
     * @param <T>       type of command result
     * @return result of command execution or throws exception if command is not registered
     * @throws UnableExecuteCommandException if command cannot be executed
     * @see CommandsFactory
     * @see Input
     * @see ActionFacade#throwFor(String, Exception)
     */
    default <T> T actCommand(final String commandId, final CommandsFactory<? extends RootCommand<?>> factory,
                             final Input<?> input) throws UnableExecuteCommandException {
        // To do action command with the given command-id, input parameter and default error processor
        return actCommand(commandId, factory, input, defaultOnErrorFor(commandId));
    }

    /**
     * To act action command with given command-id, commands factory, input parameter and command error processor.
     *
     * @param commandId the command id
     * @param factory   the commands factory to find command by id
     * @param input     the input parameter for the command execution
     * @param onError   consumer to handle command execution errors
     * @param <T>       type of command result
     * @return result of command execution or throws exception if command is not registered
     * @throws UnableExecuteCommandException if command cannot be executed
     * @see CommandsFactory#makeCommandContext(String, Input)
     * @see ActionExecutor#commitAction(ActionContext, Context)
     * @see Context#isDone()
     * @see Context#getResult()
     * @see Context#getException()
     * @see Input
     * @see ActionFacade#throwFor(String, Exception)
     */
    default <T> T actCommand(final String commandId, final CommandsFactory<? extends RootCommand<?>> factory,
                             final Input<?> input, final Consumer<Exception> onError) throws UnableExecuteCommandException {
        final Context<T> requestContext = factory.makeCommandContext(commandId, input);
        if (isNull(requestContext)) {
            // command is not registered in the factory
            getLogger().warn("Command with ID:{} is not registered in the factory:{}", commandId, factory.getName());
            return throwFor(commandId, new CommandNotRegisteredInFactoryException(commandId, factory));
        }

        final Context<T> responseContext = getActionExecutor().commitAction(ActionContext.current(), requestContext);
        if (responseContext.isDone()) {
            // success processing
            getLogger().debug("Success execution of command:{} with parameter:{}", commandId, input.value());
            // returns result of command execution
            return responseContext.getResult().orElseThrow(createThrowFor(commandId));
        } else {
            // fail processing
            onError.accept(responseContext.getException());
            // returns null if command execution failed
            return null;
        }
    }

    /**
     * To throw UnableExecuteCommandException for the command which thrown exception
     *
     * @param commandId command-id where something went wrong
     * @param e         unhandled exception occurred during command execution
     * @param <T>       type of command result or command type to execute doCommand
     * @return nothing
     */
    static <T> T throwFor(final String commandId, final Exception e) {
        throw new UnableExecuteCommandException(commandId, e);
    }

    /**
     * To create supplier to throw Runtime-exception for command from command execution context
     *
     * @param commandId command-id where something went wrong
     * @return Runtime-exception instance
     */
    static Supplier<RuntimeException> createThrowFor(final String commandId) {
        return () -> new UnableExecuteCommandException(commandId);
    }

    /**
     * To log warning message when something went wrong in command execution
     *
     * @param exception the exception occurred during command execution
     * @param commandId the command-id where something went wrong
     */
    default void logSomethingWentWrong(Exception exception, String commandId) {
        getLogger().warn("Something went wrong in command with ID:'{}'.", commandId, exception);
    }

    /**
     * To log error message when command execution failed but no exception was stored
     *
     * @param commandId the command-id where something went wrong
     * @see ActionFacade#throwFor(String, Exception)
     */
    default void failedButNoExceptionStored(String commandId) {
        getLogger().error("For command with ID:'{}' there is no exception after wrong command execution.", commandId);
        throwFor(commandId, new NullPointerException("Command failed, but Exception wasn't stored!!!"));
    }

    // private methods
    /**
     * To get default error consumer for command execution
     *
     * @param commandId the command-id where something went wrong
     * @return default error consumer for command execution
     * @see ActionFacade#logSomethingWentWrong(Exception, String)
     * @see ActionFacade#throwFor(String, Exception)
     */
    private Consumer<Exception> defaultOnErrorFor(final String commandId) {
        return exception -> {
            if (nonNull(exception)) {
                logSomethingWentWrong(exception, commandId);
                throwFor(commandId, exception);
            } else {
                failedButNoExceptionStored(commandId);
            }
        };
    }
}
