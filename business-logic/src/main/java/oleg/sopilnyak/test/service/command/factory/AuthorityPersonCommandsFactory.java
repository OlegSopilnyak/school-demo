package oleg.sopilnyak.test.service.command.factory;

import oleg.sopilnyak.test.service.command.factory.base.AbstractCommandFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.AuthorityPersonCommand;

import java.util.Collection;

/**
 * Commands factory for organization-infrastructure syb-system (authority persons)
 *
 * @see CommandsFactory
 * @see AbstractCommandFactory
 * @see AuthorityPersonCommand
 */
public class AuthorityPersonCommandsFactory<T> extends AbstractCommandFactory<T> implements CommandsFactory<T> {
    public static final String NAME = "Organization.AuthorityPersons";

    public AuthorityPersonCommandsFactory(Collection<AuthorityPersonCommand<T>> commands) {
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
