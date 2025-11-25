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
 * Type: School Command to execute the business-logic action
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
     * @see RootCommand#afterExecuteDo(Context)
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
     * @see Context#isDone()
     * @see Context#setState(Context.State)
     * @see Context.State#WORK
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
     * @see Context#isDone()
     * @see Context#getResult()
     * @see Optional#isEmpty()
     * @see #detachedResult(Object)
     * @see Context#setResult(Object)
     */
    default void afterExecuteDo(Context<T> context) {
        final String commandId = getId();
        // check if command is done and has result
        if (!context.isDone()) {
            getLog().warn("Cannot detach result data of command with id:'{}' for context:state {}", commandId, context.getState());
            return;
        }
        // getting result from context
        final Optional<T> result = context.getResult();
        // check if result is present
        if (result.isEmpty()) {
            // command execution returned nothing, no result to detach
            getLog().debug("Cannot detach result data of command: '{}' with context: no result", commandId);
        } else {
            // detach result data
            getLog().debug("Detaching result data of command: '{}'", commandId);
            final T finalResult = detachedResult(result.get());
            getLog().debug("Detached result data of command: '{}' is {}", commandId, finalResult);
            context.setState(Context.State.WORK);
            context.setResult(finalResult);
        }
    }

    /**
     * To get reference to the logger uses in the command
     *
     * @return reference to the logger
     * @see RootCommand#doCommand(Context)
     * @see RootCommand#undoCommand(Context)
     * @see RootCommand#afterExecuteDo(Context)
     */
    Logger getLog();

    /**
     * To detach command result data from persistence layer
     *
     * @param result result data to detach
     * @return detached result data
     * @see RootCommand#afterExecuteDo(Context)
     */
    T detachedResult(T result);

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
