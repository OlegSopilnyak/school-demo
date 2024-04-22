package oleg.sopilnyak.test.service.command.factory;

import oleg.sopilnyak.test.service.command.factory.base.AbstractCommandFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.base.ProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.type.PrincipalProfileCommand;

import java.util.Collection;

/**
 * Commands factory for principal profiles syb-system
 *
 * @see CommandsFactory
 * @see AbstractCommandFactory
 * @see PrincipalProfileCommand
 */
public class PrincipalProfileCommandsFactory<T> extends ProfileCommandsFactory<T> implements CommandsFactory<T> {
    public static final String NAME = "Principal-Profiles";

    public PrincipalProfileCommandsFactory(Collection<PrincipalProfileCommand<T>> commands) {
        super(commands);
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