package oleg.sopilnyak.test.service.command.executable.core.executor;

import static oleg.sopilnyak.test.service.message.CommandMessage.Direction.DO;
import static oleg.sopilnyak.test.service.message.CommandMessage.Direction.UNDO;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandMessage;
import oleg.sopilnyak.test.service.message.DoCommandMessage;
import oleg.sopilnyak.test.service.message.UndoCommandMessage;

import java.util.UUID;
import java.util.function.Predicate;
import org.slf4j.Logger;

/**
 * Facade: The main engine to execute school command activities.
 * It is using to execute school-commands that are used in the business logic.
 *
 * @see Context
 * @see Context#getCommand()
 */
public interface CommandActionExecutor {
    // Predicates for valid message direction
    Predicate<CommandMessage<?>> IS_VALID_MESSAGE_CONTEXT = message -> message.getContext() != null;
    Predicate<CommandMessage<?>> IS_VALID_MESSAGE_CONTEXT_COMMAND = message -> message.getContext().getCommand() != null;
    Predicate<CommandMessage<?>> IS_NULL_MESSAGE_DIRECTION = message -> message.getDirection() == null;
    Predicate<CommandMessage<?>> IS_DO_MESSAGE_DIRECTION = message -> message.getDirection() == DO;
    Predicate<CommandMessage<?>> IS_UNDO_MESSAGE_DIRECTION = message -> message.getDirection() == UNDO;
    Predicate<CommandMessage<?>> isInvalidMessage = IS_VALID_MESSAGE_CONTEXT.and(IS_VALID_MESSAGE_CONTEXT_COMMAND).and(IS_NULL_MESSAGE_DIRECTION);
    Predicate<CommandMessage<?>> isValidMessageDirection = IS_DO_MESSAGE_DIRECTION.or(IS_UNDO_MESSAGE_DIRECTION);

    /**
     * To initialize school-commands executor
     */
    void initialize();

    /**
     * To shut down school-commands executor
     */
    void shutdown();

    /**
     * To process command message, executing command from command-context of the message
     *
     * @param message the processing command message
     * @param <T>     type of command result
     * @return processed command message
     * @see CommandMessage
     * @see Context
     * @see Context#getCommand()
     * @see oleg.sopilnyak.test.service.command.type.core.CommandExecutable#doCommand(Context)
     * @see oleg.sopilnyak.test.service.command.type.core.CommandExecutable#undoCommand(Context)
     */
    default <T> CommandMessage<T> processActionCommand(final CommandMessage<T> message) {
        //
        // This method can be overridden to process command messages by another way
        //
        // Getting the reference to command context of the message
        final var context = message.getContext();
        // Validate the message and it's direction
        if (isInvalidMessage.test(message)) {
            getLogger().warn("Command message direction is not defined in: '{}'.", message);
            context.failed(new IllegalArgumentException("Command message direction is not defined properly."));
        } else {
            // getting command-id to process
            final String commandId = context.getCommand().getId();
            // Getting message's direction
            final var direction = message.getDirection();
            // Execute or rollback command with context according to the message-direction
            switch (direction) {
                // Execute the command with the given context
                case DO -> context.getCommand().doCommand(context);
                // Rolling back execution of the command with the given context
                case UNDO -> context.getCommand().undoCommand(context);
                // Unknown direction detected
                default -> {
                    getLogger().warn("Unknown message direction: '{}' for command '{}'.", direction, commandId);
                    context.failed(new IllegalArgumentException("Unknown message direction: " + direction));
                }
            }
        }
        // returns the message after processing
        return message;
    }

    /**
     * To get the logger of the executor implementation
     *
     * @return logger instance
     */
    Logger getLogger();

    /**
     * To do (commit) processing with the action context and command context
     *
     * @param actionContext  the action context
     * @param commandContext the command context
     * @param <T>            type of do command execution result
     * @return command-context after undo command execution
     * @see ActionContext
     * @see Context
     */
    default <T> Context<T> commitAction(final ActionContext actionContext, final Context<T> commandContext) {
        return processActionCommand(buildMessage(actionContext, commandContext, DO)).getContext();
    }

    /**
     * To undo (rollback) processing with the action context and command context
     *
     * @param actionContext  the action context
     * @param commandContext the command context
     * @return command-context after undo command execution
     * @see ActionContext
     * @see Context
     */
    default <T> Context<T> rollbackAction(final ActionContext actionContext, final Context<T> commandContext) {
        return processActionCommand(buildMessage(actionContext, commandContext, CommandMessage.Direction.UNDO)).getContext();
    }

    /**
     * To validateInput input message's direction before processing
     *
     * @param message input message to check
     * @param <T>     type of do command execution result
     */
    default <T> void validateInput(CommandMessage<T> message) {
        if (IS_NULL_MESSAGE_DIRECTION.test(message)) {
            throw new IllegalArgumentException("Message direction is not defined.");
        } else if (isValidMessageDirection.negate().test(message)) {
            throw new IllegalArgumentException("Unknown message direction: " + message.getDirection());
        }
    }

    /**
     * To build the command-message instance to process
     *
     * @param actionContext  the action context
     * @param commandContext the command context
     * @param direction      the direction of message processing
     * @param <T>            type of do command execution result
     * @return built message instance
     * @see ActionContext
     * @see Context
     * @see CommandMessage.Direction
     */
    private <T> CommandMessage<T> buildMessage(
            final ActionContext actionContext, final Context<T> commandContext, final CommandMessage.Direction direction
    ) {
        return switch (direction) {
            // message to commands execution subsystem (to execute command using context)
            case DO -> DoCommandMessage.<T>builder().actionContext(actionContext).context(commandContext)
                    .correlationId(UUID.randomUUID().toString()).build();
            // message to commands execution subsystem (to rollback execution of command using context)
            case UNDO -> UndoCommandMessage.builder().actionContext(actionContext).context(commandContext)
                    .correlationId(UUID.randomUUID().toString()).build();
            case null, default -> new BaseCommandMessage<>("bad-correlation-id", actionContext, commandContext) {
                @Override
                public Direction getDirection() {
                    return Direction.UNKNOWN;
                }
            };
        };
    }
}
