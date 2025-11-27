package oleg.sopilnyak.test.service.command.executable;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.DoCommandMessage;
import oleg.sopilnyak.test.service.message.UndoCommandMessage;

import java.util.UUID;
import org.slf4j.Logger;

/**
 * The main engine for execute school actions.
 * It is used to execute commands that are defined in the business logic.
 */
public interface ActionExecutor {
    /**
     * To get the logger of the executor implementation
     *
     * @return logger instance
     */
    Logger getLogger();

    /**
     * To do (commit) action with the action context and command context
     *
     * @param actionContext  the action context
     * @param commandContext the command context
     * @param <T>            type of do command execution result
     * @return command-context after undo command execution
     * @see ActionContext
     * @see Context
     */
    default <T> Context<T> commitAction(final ActionContext actionContext, final Context<T> commandContext) {
        final DoCommandMessage<T> message = DoCommandMessage.<T>builder()
                .actionContext(actionContext).context(commandContext)
                .correlationId(UUID.randomUUID().toString())
                .build();
        return processActionCommand(message).getContext();
    }

    /**
     * To undo (rollback) action with the action context and command context
     *
     * @param actionContext  the action context
     * @param commandContext the command context
     * @return command-context after undo command execution
     * @see ActionContext
     * @see Context
     */
    default Context<?> rollbackAction(final ActionContext actionContext, final Context<?> commandContext) {
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
     * @param <T>     type of command result
     * @return processed command message
     * @see BaseCommandMessage
     */
    default <T> BaseCommandMessage<T> processActionCommand(final BaseCommandMessage<T> message) {
        // This method can be overridden to process command messages

        // Validate the message and its context
        if (!isNull(message.getContext().getCommand()) && isNull(message.getDirection())) {
            getLogger().warn("Command message direction is not defined in: '{}'.", message);
            message.getContext().failed(new IllegalArgumentException("Command message direction is not defined."));
            return message;
        }

        // Get the command from the message context
        final RootCommand<T> command = message.getContext().getCommand();

        // Execute or rollback command based on the direction
        switch (message.getDirection()) {
            case DO:
                // Execute the command with the given context
                command.doCommand(message.getContext());
                break;
            case UNDO:
                // Rollback the command with the given context
                command.undoCommand(message.getContext());
                break;
            default:
                getLogger().warn("Unknown message direction: '{}' for command '{}'.", message.getDirection(), command.getId());
                message.getContext().failed(new IllegalArgumentException("Unknown message direction: " + message.getDirection()));
        }
        return message;
    }
}
