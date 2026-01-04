package oleg.sopilnyak.test.service.exception;

import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;

/**
 * Exception throws when command is not registered in the command registry factory
 * @see CommandsFactory#getName()
 */
public class CommandNotRegisteredInFactoryException extends Exception {
    public <T extends RootCommand<?>> CommandNotRegisteredInFactoryException(String commandId,
                                                                          CommandsFactory<T> factory) {
        super("Command '" + commandId + "' is not registered in factory :" + factory.getName());
    }
}
