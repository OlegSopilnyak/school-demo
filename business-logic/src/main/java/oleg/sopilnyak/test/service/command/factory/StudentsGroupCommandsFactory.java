package oleg.sopilnyak.test.service.command.factory;

import oleg.sopilnyak.test.service.command.factory.base.AbstractCommandFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.StudentsGroupCommand;

import java.util.Collection;

/**
 * Commands factory for organization-infrastructure syb-system (students groups)
 *
 * @see CommandsFactory
 * @see AbstractCommandFactory
 * @see StudentsGroupCommand
 */
public class StudentsGroupCommandsFactory<T> extends AbstractCommandFactory<T> implements CommandsFactory<T> {
    public static final String NAME = "Organization.StudentsGroups";

    public StudentsGroupCommandsFactory(Collection<StudentsGroupCommand<T>> commands) {
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
