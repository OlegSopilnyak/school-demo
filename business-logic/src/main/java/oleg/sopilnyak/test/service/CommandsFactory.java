package oleg.sopilnyak.test.service;

import oleg.sopilnyak.test.service.command.SchoolCommand;

/**
 * Factory: the factory of commands
 */
public interface CommandsFactory {
    /**
     * To get command instance by commandId
     *
     * @param commandId command-id
     * @return command instance
     * @param <T> type of command execution result
     */
   <T> SchoolCommand<T> command(String commandId);
}
