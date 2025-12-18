package oleg.sopilnyak.test.service.command.factory.profile;

import oleg.sopilnyak.test.service.command.factory.base.AbstractCommandFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.base.ProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;

import java.util.Collection;

/**
 * Commands factory for principal profiles syb-system
 *
 * @see CommandsFactory
 * @see AbstractCommandFactory
 * @see PrincipalProfileCommand
 */
public class PrincipalProfileCommandsFactory extends ProfileCommandsFactory<PrincipalProfileCommand<?>> {
    public static final String NAME = "Principal-Profiles";

    public PrincipalProfileCommandsFactory(Collection<PrincipalProfileCommand<?>> commands) {
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
        return (Class<F>) PrincipalProfileCommand.class;
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
