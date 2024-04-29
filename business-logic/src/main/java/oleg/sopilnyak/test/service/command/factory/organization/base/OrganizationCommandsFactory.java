package oleg.sopilnyak.test.service.command.factory.organization.base;

import oleg.sopilnyak.test.service.command.factory.base.AbstractCommandFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.organization.base.OrganizationCommand;

import java.util.Collection;

/**
 * Commands factory for organization-infrastructure syb-system
 */
public abstract class OrganizationCommandsFactory<T> extends AbstractCommandFactory<T> implements CommandsFactory<T> {
    protected OrganizationCommandsFactory(Collection<? extends OrganizationCommand<T>> commands) {
        super.applyFactoryCommands(commands);
    }

}
