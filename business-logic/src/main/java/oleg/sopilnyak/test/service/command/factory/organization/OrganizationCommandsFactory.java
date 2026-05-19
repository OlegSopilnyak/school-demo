package oleg.sopilnyak.test.service.command.factory.organization;

import oleg.sopilnyak.test.service.command.factory.AbstractCommandFactory;
import oleg.sopilnyak.test.service.command.factory.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.organization.base.OrganizationCommand;

import java.util.Collection;

/**
 * Commands factory for organization-infrastructure syb-system
 */
public abstract class OrganizationCommandsFactory<T extends OrganizationCommand<?>>
        extends AbstractCommandFactory<T>
        implements CommandsFactory<T> {
    protected OrganizationCommandsFactory(Collection<T> commands) {
        super.applyFactoryCommands(commands);
    }
}
