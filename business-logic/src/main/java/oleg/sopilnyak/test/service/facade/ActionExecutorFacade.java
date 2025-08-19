package oleg.sopilnyak.test.service.facade;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.exception.CommandNotRegisteredInFactoryException;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.DoCommandMessage;
import oleg.sopilnyak.test.service.message.UndoCommandMessage;
import org.slf4j.Logger;

/**
 * Facade Utility: Facade for manage execution of school actions activity
 *
 * @see ActionContext
 * @see Context
 */
public interface ActionExecutorFacade {
    /**
     * To get the logger of the facade
     *
     * @return logger instance
     */
    Logger getLogger();

    /**
     * To get the commands factory of the facade to deal with commands
     *
     * @return factory of commands instance
     */
    CommandsFactory<? extends RootCommand<?>> getFactory();


    /** To do action command with the given command-id and input parameter.
     *
     * @param commandId the command id
     * @param input     the input parameter for the command execution
     * @param <T>       type of command result
     * @return result of command execution or throws exception if command is not registered
     * @throws UnableExecuteCommandException if command cannot be executed
     * @see Input
     * @see ActionExecutorFacade#throwFor(String, Exception)
     */
    default <T> T doActionCommand(final String commandId, final Input<?> input) throws UnableExecuteCommandException {
        final Consumer<Exception> defaultOnCommandErrorConsumer = exception -> {
            if (nonNull(exception)) {
                getLogger().warn("Something went wrong in command with ID:'{}'.", commandId, exception);
                throwFor(commandId, exception);
            } else {
                getLogger().error("For command with ID:'{}' there is no exception after wrong command execution.", commandId);
                throwFor(commandId, new NullPointerException("Command fail Exception was not stored!!!"));
            }
        };
        // To do action command with the given command-id, input parameter and default error processor
        return doActionCommand(commandId, input, defaultOnCommandErrorConsumer);
    }

    /** To do action command with the given command-id, input parameter and command error processor.
     *
     * @param commandId the command id
     * @param input     the input parameter for the command execution
     * @param onCommandError consumer to handle command error
     * @param <T>       type of command result
     * @return result of command execution or throws exception if command is not registered
     * @throws UnableExecuteCommandException if command cannot be executed
     * @see Input
     */
    default <T> T doActionCommand(
            final String commandId, final Input<?> input, final Consumer<Exception> onCommandError
    ) throws UnableExecuteCommandException {
        final Context<T> requestContext = getFactory().makeCommandContext(commandId, input);
        if (isNull(requestContext)) {
            // command is not registered in the factory
            getLogger().warn("Command with ID:{} is not registered in the factory:{}", commandId, getFactory().getName());
            return throwFor(commandId, new CommandNotRegisteredInFactoryException(commandId, getFactory()));
        }

        final Context<T> responseContext = doAction(ActionContext.current(), requestContext);
        if (responseContext.isDone()) {
            // success processing
            getLogger().debug("Success execution of command:{} with parameter:{}", commandId, input.value());
            // returns result of command execution
            return responseContext.getResult().orElseThrow(createThrowFor(commandId));
        } else {
            // fail processing
            onCommandError.accept(responseContext.getException());
            // returns null if command execution failed
            return null;
        }
    }
    /**
     * To do (commit) action with the action context and command context
     *
     * @param actionContext the action context
     * @param commandContext the command context
     * @param <T> type of do command execution result
     * @return command-context after undo command execution
     * @see ActionContext
     * @see Context
     */
    default <T> Context<T> doAction(final ActionContext actionContext, final Context<T> commandContext) {
        final DoCommandMessage<T> message = DoCommandMessage.<T>builder()
                .actionContext(actionContext).context(commandContext)
                .correlationId(UUID.randomUUID().toString())
                .build();
        return processActionCommand(message).getContext();
    }

    /**
     * To undo (rollback) action with the action context and command context
     *
     * @param actionContext the action context
     * @param commandContext the command context
     * @return command-context after undo command execution
     * @see ActionContext
     * @see Context
     */
    default  Context<Void> undoAction(final ActionContext actionContext, final Context<Void> commandContext) {
        final UndoCommandMessage message = UndoCommandMessage.builder()
                .actionContext(actionContext).context(commandContext)
                .correlationId(UUID.randomUUID().toString())
                .build();
        return processActionCommand(message).getContext();
    }

    /**
     * To process action command message
     *
     * @param message the action command message
     * @param <T> type of command result
     * @return processed command message
     * @see BaseCommandMessage
     */
    default <T> BaseCommandMessage<T> processActionCommand(final BaseCommandMessage<T> message) {
        // This method can be overridden to process command messages
        switch (message.getDirection()) {
            case DO:
                // Execute the command with the given context
                message.getContext().getCommand().doCommand(message.getContext());
                break;
            case UNDO:
                // Rollback the command with the given context
                message.getContext().getCommand().undoCommand(message.getContext());
                break;
            default:
                getLogger().warn("Unknown command direction: '{}'.", message.getDirection());
                throw new IllegalArgumentException("Unknown command direction: " + message.getDirection());
        }
        return message;
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
}
