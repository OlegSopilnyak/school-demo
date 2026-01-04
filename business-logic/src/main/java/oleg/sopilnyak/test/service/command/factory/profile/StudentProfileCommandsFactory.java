package oleg.sopilnyak.test.service.command.factory.profile;

import oleg.sopilnyak.test.service.command.factory.base.AbstractCommandFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.base.ProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;

import java.util.Collection;

/**
 * Commands factory for student profiles syb-system
 *
 * @see CommandsFactory
 * @see AbstractCommandFactory
 * @see StudentProfileCommand
 */
public class StudentProfileCommandsFactory extends ProfileCommandsFactory<StudentProfileCommand<?>> {
    public static final String NAME = "Student-Profiles";

    public StudentProfileCommandsFactory(Collection<StudentProfileCommand<?>> commands) {
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
        return (Class<F>) StudentProfileCommand.class;
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
