package oleg.sopilnyak.test.service.command.factory.base;

import oleg.sopilnyak.test.service.command.type.base.RootCommand;

import java.util.Collection;

/**
 * Type Container: the factory of commands
 *
 * @param <T> type of factory's commands
 */
public interface CommandsFactory<T extends RootCommand> {

    /**
     * To get the commandIds of registered commands
     *
     * @return commandIds of registered commands
     */
    Collection<String> commandIds();

    /**
     * To get command instance by commandId
     *
     * @param commandId command-id
     * @return command instance or null if not registered
     */
    T command(String commandId);

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
     */
    int getSize();
}
