package oleg.sopilnyak.test.service.command.factory.profile;

import oleg.sopilnyak.test.service.command.factory.base.AbstractCommandFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.profile.base.ProfileCommandsFactory;
import oleg.sopilnyak.test.service.command.type.profile.StudentProfileCommand;

import java.util.Collection;

/**
 * Commands factory for student profiles syb-system
 *
 * @see CommandsFactory
 * @see AbstractCommandFactory
 * @see StudentProfileCommand
 */
public class StudentProfileCommandsFactory<T> extends ProfileCommandsFactory<T> implements CommandsFactory<T> {
    public static final String NAME = "Student-Profiles";

    public StudentProfileCommandsFactory(Collection<StudentProfileCommand<T>> commands) {
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
