package oleg.sopilnyak.test.service.command.type.base;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.service.command.executable.sys.BasicCommand;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;

import java.util.Optional;
import org.slf4j.Logger;

/**
 * Type: School Command to execute the business-logic processing
 *
 * @param <T> the type of command execution result
 */
public interface RootCommand<T> extends CommandExecutable<T>, NestedCommand<T> {
    /**
     * The class of commands family, the command is belonged to
     *
     * @return command family class value
     * @param <F> class of command's family
     * @see BasicCommand#self()
     */
    default <F extends RootCommand> Class<F> commandFamily() {
        throw new UnsupportedOperationException("Please declare commands family type.");
    }

    /**
     * The name of command bean in spring beans factory, should override in concrete command
     *
     * @return spring name of the command
     * @see BasicCommand#self()
     */
    default String springName() {
        throw new UnsupportedOperationException("Please declare the name of command in spring beans factory.");
    }

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
     * @see Input#isEmpty()
     */
    default <I> void checkNullParameter(Input<I> parameter) {
        if (isNull(parameter) || parameter.isEmpty()) {
            throw new NullPointerException("Wrong input parameter value (cannot be null or empty).");
        }
    }

    /**
     * To create command's context without parameters
     *
     * @return context instance
     * @see CommandContext#builder()
     * @see CommandContext.CommandContextBuilder#command(RootCommand)
     * @see CommandContext.CommandContextBuilder#build()
     * @see Context#setState(Context.State)
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
     * @see CommandContext#builder()
     * @see CommandContext.CommandContextBuilder#command(RootCommand)
     * @see CommandContext.CommandContextBuilder#redoParameter(Input)
     * @see CommandContext.CommandContextBuilder#build()
     * @see Context#setState(Context.State)
     * @see Context.State#INIT
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
     * @see RootCommand#createContext()
     * @see Context#failed(Exception)
     */
    @Override
    default Context<T> createFailedContext(Exception cause) {
        return createContext().failed(cause);
    }

    /**
     * Reference to the current command for operations with the command's entities in transaction possibility
     *
     * @return the reference to the current command from spring beans factory
     * @see RootCommand#doCommand(Context)
     * @see RootCommand#undoCommand(Context)
     */
    @Override
    default RootCommand<T> self() {
        return this;
    }

    /**
     * To execute command logic with context
     *
     * @param context context of redo execution
     * @see Context#isReady()
     * @see Context#setState(Context.State)
     * @see Context.State#WORK
     * @see RootCommand#self()
     * @see CommandExecutable#executeDo(Context)
     * @see RootCommand#afterExecute(Context)
     */
    @Override
    default void doCommand(Context<T> context) {
        if (context.isReady()) {
            // start executing command-do with correct context state
            context.setState(Context.State.WORK);
            self().executeDo(context);
            afterExecute(context);
        } else {
            getLog().warn("Cannot do redo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        }
    }

    /**
     * To detach command result data from persistence layer after command execution
     *
     * @param context context of command execution
     * @see Context#isDone()
     * @see Context#getResult()
     * @see Optional#isEmpty()
     * @see Context#setResult(Object)
     */
    default void afterExecute(Context<T> context) {
        final String commandId = getId();
        // check if command is done and has result
        if (context.isDone()) {
            // getting result from context
            final Optional<T> result = context.getResult();
            // check if result is present
            if (result.isEmpty()) {
                // command execution returned nothing, no result to store
                getLog().debug("Won't store result of command: '{}' to context holder: there's no result", commandId);
            } else {
                // store result data
                getLog().debug("Getting result of command: '{}' execution", commandId);
                final T finalResult = result.orElse(null);
                getLog().debug(
                        "Saving to context holder result data of command: '{}' result {}", commandId, finalResult
                );
                context.setResult(finalResult);
            }
        } else {
            getLog().warn("Cannot make and apply result data of command with id:'{}' because of context:state {}", commandId, context.getState());
        }
    }

    /**
     * To rollback command's execution according to command context
     *
     * @param context context of undo execution
     * @see Context#isDone()
     * @see Context#setState(Context.State)
     * @see Context.State#WORK
     * @see CommandExecutable#executeUndo(Context)
     * @see RootCommand#self()
     * @see RootCommand#afterRollback(Context)
     */
    @Override
    default void undoCommand(Context<?> context) {
        if (context.isDone()) {
            // start executing command-undo with correct context state
            context.setState(Context.State.WORK);
            self().executeUndo(context);
            afterRollback(context);
        } else {
            getLog().warn("Cannot do undo of command {} with context:state '{}'", getId(), context.getState());
            context.setState(Context.State.FAIL);
        }
    }

    /**
     * To do things after successful rollback
     *
     * @param context context of undo execution
     * @see CommandExecutable#undoCommand(Context)
     */
    default void afterRollback(Context<?> context) {
        // clearing command execution result
        context.setResult(null);
    }

    /**
     * To get reference to the logger uses in the command
     *
     * @return reference to the logger
     * @see RootCommand#doCommand(Context)
     * @see RootCommand#undoCommand(Context)
     * @see RootCommand#afterExecute(Context)
     */
    Logger getLog();

    // For commands playing as a nested-command

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor visitor of prepared contexts
     * @param input   Macro-Command input for command-context preparation
     * @return prepared for nested command context
     * @see NestedCommand#acceptPreparedContext(PrepareNestedContextVisitor, Input)
     * @see PrepareNestedContextVisitor#prepareContext(RootCommand, Input)
     */
    @Override
    default Context<T> acceptPreparedContext(PrepareNestedContextVisitor visitor, Input<?> input) {
        // it's using visitor for preparing concrete command-context
        return visitor.prepareContext(this, input);
    }

}
