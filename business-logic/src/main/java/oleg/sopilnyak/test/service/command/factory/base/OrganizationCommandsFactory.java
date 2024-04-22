package oleg.sopilnyak.test.service.command.factory.base;

import oleg.sopilnyak.test.service.command.type.base.command.OrganizationCommand;

import java.util.Collection;

/**
 * Commands factory for organization-infrastructure syb-system
 */
public abstract class OrganizationCommandsFactory<T> extends AbstractCommandFactory<T> implements CommandsFactory<T> {
    public OrganizationCommandsFactory(Collection<? extends OrganizationCommand<T>> commands) {
        super.applyFactoryCommands(commands);
    }

}
