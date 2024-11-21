package oleg.sopilnyak.test.service.command.factory.profile.base;

import oleg.sopilnyak.test.service.command.factory.base.AbstractCommandFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;

import java.util.Collection;

/**
 * Commands factory for profiles syb-system
 *
 * @see CommandsFactory
 * @see AbstractCommandFactory
 * @see ProfileCommand
 */
public abstract class ProfileCommandsFactory<T extends ProfileCommand<?>>
        extends AbstractCommandFactory<T> {
    protected ProfileCommandsFactory(Collection<T> commands) {
        super.applyFactoryCommands(commands);
    }
}
