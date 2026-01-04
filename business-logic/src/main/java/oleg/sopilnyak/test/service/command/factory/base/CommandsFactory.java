package oleg.sopilnyak.test.service.command.factory.base;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;

import java.util.Collection;

/**
 * Type Container: the factory of commands
 *
 * @param <C> type of factory's commands
 */
public interface CommandsFactory<C extends RootCommand<?>> {
    /**
     * To get command instance by command-id
     *
     * @param commandId command-id
     * @return command instance or null if not registered
     */
    C command(String commandId);

    /**
     * The class of commands family, the commands are belonged to
     *
     * @return command family class value
     * @param <F> class of command's family
     * @see RootCommand#commandFamily()
     */
    default <F extends RootCommand> Class<F> commandFamily() {
        throw new UnsupportedOperationException("Please declare commands family type.");
    }

    /**
     * To make command execution context by command-id and input
     *
     * @param commandId command-id
     * @param input     input parameter for the command execution
     * @param <I>       type of input parameter
     * @param <R>       type of command result
     * @return command context or empty if not registered
     * @see CommandsFactory#command(String)
     * @see RootCommand#createContext(Input)
     * @see Context
     */
    @SuppressWarnings("unchecked")
    default <I, R> Context<R> makeCommandContext(final String commandId, final Input<I> input) {
        final C command = command(commandId);
        return isNull(command) ? null : (Context<R>) command.createContext(input);
    }

    /**
     * To get sorted ASC command ids of registered commands
     *
     * @return sorted ids of registered commands
     */
    Collection<String> commandIds();

    /**
     * To get the name of the commands factory
     *
     * @return value
     */
    String getName();

    /**
     * To get the quantity of commands in the factory
     *
     * @return quantity of commands in the factory
     * @see CommandsFactory#commandIds()
     */
    default int getSize() {
        return commandIds().size();
    }
}
