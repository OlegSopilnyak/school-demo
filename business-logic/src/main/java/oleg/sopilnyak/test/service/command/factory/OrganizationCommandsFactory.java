package oleg.sopilnyak.test.service.command.factory;

import oleg.sopilnyak.test.service.command.factory.base.AbstractCommandFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.OrganizationCommand;

import java.util.Collection;

/**
 * Commands factory for organization-infrastructure syb-system
 */
public class OrganizationCommandsFactory<T> extends AbstractCommandFactory<T> implements CommandsFactory<T> {
    public static final String NAME = "Organization";

    public OrganizationCommandsFactory(Collection<OrganizationCommand<T>> commands) {
        super.applyFactoryCommands(commands);
    }

    /**
     * To get the name of the commands factory
     *
     * @return value
     */
    public String getName() {
        return NAME;
    }

}
