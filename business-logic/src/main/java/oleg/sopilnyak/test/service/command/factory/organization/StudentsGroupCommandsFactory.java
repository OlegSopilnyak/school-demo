package oleg.sopilnyak.test.service.command.factory.organization;

import oleg.sopilnyak.test.service.command.factory.base.AbstractCommandFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.factory.organization.base.OrganizationCommandsFactory;
import oleg.sopilnyak.test.service.command.type.organization.StudentsGroupCommand;

import java.util.Collection;

/**
 * Commands factory for organization-infrastructure syb-system (students groups)
 *
 * @see CommandsFactory
 * @see AbstractCommandFactory
 * @see StudentsGroupCommand
 */
public class StudentsGroupCommandsFactory extends OrganizationCommandsFactory<StudentsGroupCommand<?>> {
    public static final String NAME = "Organization.StudentsGroups";

    public StudentsGroupCommandsFactory(Collection<StudentsGroupCommand<?>> commands) {
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
