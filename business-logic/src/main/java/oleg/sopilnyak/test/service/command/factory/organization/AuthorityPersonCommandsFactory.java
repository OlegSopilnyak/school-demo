package oleg.sopilnyak.test.service.command.factory.organization;

import oleg.sopilnyak.test.service.command.factory.base.AbstractCommandFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.base.OrganizationCommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;

import java.util.Collection;

/**
 * Commands factory for organization-infrastructure syb-system (authority persons)
 *
 * @see CommandsFactory
 * @see AbstractCommandFactory
 * @see AuthorityPersonCommand
 */
public class AuthorityPersonCommandsFactory extends OrganizationCommandsFactory<AuthorityPersonCommand<?>> {
    public static final String NAME = "Organization.AuthorityPersons";

    public AuthorityPersonCommandsFactory(Collection<AuthorityPersonCommand<?>> commands) {
        super(commands);
    }

    /**
     * The class of commands family, the commands are belonged to
     *
     * @return command family class value
     * @see RootCommand#commandFamily()
     */
    @Override
    @SuppressWarnings("unchecked")
    public <F extends RootCommand> Class<F> commandFamily() {
        return (Class<F>) AuthorityPersonCommand.class;
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
