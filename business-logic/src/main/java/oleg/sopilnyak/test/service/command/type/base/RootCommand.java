package oleg.sopilnyak.test.service.command.type.base;

import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import org.slf4j.Logger;

import java.util.Optional;

import static java.util.Objects.isNull;

/**
 * Type: School Command to execute the business-logic action
 *
 * @param <T> the type of command execution (do) result
 */
public interface RootCommand<T> extends CommandExecutable<T>, NestedCommand<T> {
    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    Logger getLog();

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    String getId();

    /**
     * To get mapper instance for business-message-payload transformation
     *
     * @return mapper instance
     * @see BusinessMessagePayloadMapper
     */
    default BusinessMessagePayloadMapper getPayloadMapper() {
        return null;
    }

    /**
     * To check nullable of the input parameter
     *
     * @param parameter input value to check
     */
    default <I> void checkNullParameter(Input<I> parameter) {
        if (isNull(parameter) || parameter.isEmpty()) {
            throw new NullPointerException("Wrong input parameter value null");
        }
    }

    /**
     * To create command's context without parameters
     *
     * @return context instance
     * @see Context
     * @see CommandContext
     * @see Context.State#INIT
     */
    @Override
    default Context<T> createContext() {
        final Context<T> context = CommandContext.<T>builder().command(this).build();
        context.setState(Context.State.INIT);
        return context;
    }

    /**
     * To create command's context with doParameter
     *
     * @param parameter context's doParameter input value
     * @return context instance
     * @see Input
     * @see Context
     * @see Context#getRedoParameter()
     * @see CommandContext
     * @see Context.State#READY
     */
    @Override
    default Context<T> createContext(Input<?> parameter) {
        final Context<T> context = CommandContext.<T>builder().command(this).redoParameter(parameter).build();
        context.setState(Context.State.INIT);
        context.setState(Context.State.READY);
        return context;
    }

    /**
     * To create failed context for the nested-command
     *
     * @param cause cause of fail
     * @return instance of failed command-context
     */
    @Override
    default Context<T> createFailedContext(Exception cause) {
        return createContext().failed(cause);
    }

    /**
     * Reference to the current command for operations with the command's entities in transaction possibility
     *
     * @return the reference to the current command from spring beans factory
     * @see org.springframework.context.ApplicationContext
     * @see RootCommand#doCommand(Context)
     * @see RootCommand#undoCommand(Context)
     */
    default RootCommand<T> self() {
        return this;
    }

    /**
     * To execute command logic with context
     *
     * @param context context of redo execution
     * @see Context
     * @see CommandExecutable#executeDo(Context)
     * @see RootCommand#self()
     */
    @Override
    default void doCommand(Context<T> context) {
        if (context.isReady()) {
            // start executing command-do with correct context state
            context.setState(Context.State.WORK);
            self().executeDo(context);
            afterExecuteDo(context);
        } else {
            getLog().warn("Cannot do redo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        }
    }

    /**
     * To rollback command's execution according to command context
     *
     * @param context context of undo execution
     * @see Context
     * @see Context#getUndoParameter()
     * @see CommandExecutable#executeUndo(Context)
     * @see RootCommand#self()
     */
    @Override
    default void undoCommand(Context<?> context) {
        if (context.isDone()) {
            // start executing command-undo with correct context state
            context.setState(Context.State.WORK);
            self().executeUndo(context);
        } else {
            getLog().warn("Cannot do undo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        }
    }

    /**
     * To detach command result data from persistence layer after command execution
     *
     * @param context context of command execution
     * @see Context
     * @see Context#isDone()
     * @see Context#getResult()
     * @see #detachedResult(Object)
     */
    default void afterExecuteDo(Context<T> context) {
        // check if command is done and has result
        if (!context.isDone()) {
            getLog().warn("Cannot detach result data of command with id:'{}' for context:state {}", getId(), context.getState());
            return;
        }
        // getting result from context
        final Optional<T> result = context.getResult();
        // check if result is present
        if (result.isEmpty()) {
            // command execution returned nothing, no result to detach
            getLog().debug("Cannot detach result data of command: '{}' with context: no result", getId());
        } else {
            // detach result data
            getLog().debug("Detaching result data of command: '{}'", getId());
            context.setResult(detachedResult(result.get()));
        }
    }

    /**
     * To detach command result data from persistence layer
     *
     * @param result result data to detach
     * @return detached result data
     * @see RootCommand#afterExecuteDo(Context)
     */
    T detachedResult(T result);

    // For commands playing as a Nested Command

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareNestedContextVisitor#prepareContext(RootCommand, Input)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareNestedContextVisitor visitor, final Input<?> macroInputParameter) {
        return visitor.prepareContext(this, macroInputParameter);
    }

}
