package oleg.sopilnyak.test.service.command.factory.profile.base;

import oleg.sopilnyak.test.service.command.factory.base.AbstractCommandFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;

import java.util.Collection;

/**
 * Commands factory for profiles syb-system
 */
public abstract class ProfileCommandsFactory<T> extends AbstractCommandFactory<T> implements CommandsFactory<T> {
    protected ProfileCommandsFactory(Collection<? extends ProfileCommand<T>> commands) {
        super.applyFactoryCommands(commands);
    }
}
