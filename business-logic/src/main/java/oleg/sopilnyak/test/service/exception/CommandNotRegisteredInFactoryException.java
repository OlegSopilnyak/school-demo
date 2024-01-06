package oleg.sopilnyak.test.service.exception;

import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;

/**
 * Exception throws when command is not registered in the command registry factory
 * @see CommandsFactory#getName()
 */
public class CommandNotRegisteredInFactoryException extends Exception {
    public CommandNotRegisteredInFactoryException(String commandId, CommandsFactory<?> factory) {
        super("Command '" + commandId + "' is not registered in factory :" + factory.getName());
    }
}
