package oleg.sopilnyak.test.service.command.factory.base;

import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

/**
 * Factory: the factory of commands
 */
public interface CommandsFactory<T> {

    /**
     * To get command instance by commandId
     *
     * @param commandId command-id
     * @return command instance or null if not registered
     */
    SchoolCommand<T> command(String commandId);

    /**
     * To get the name of the commands factory
     * @return value
     */
    String getName();
}
