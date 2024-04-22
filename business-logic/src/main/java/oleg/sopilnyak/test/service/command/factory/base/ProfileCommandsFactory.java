package oleg.sopilnyak.test.service.command.factory.base;

import oleg.sopilnyak.test.service.command.type.base.command.ProfileCommand;

import java.util.Collection;

/**
 * Commands factory for profiles syb-system
 */
public abstract class ProfileCommandsFactory<T> extends AbstractCommandFactory<T> implements CommandsFactory<T> {
    public ProfileCommandsFactory(Collection<? extends ProfileCommand<T>> commands) {
        super.applyFactoryCommands(commands);
    }
}
