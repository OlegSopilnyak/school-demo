package oleg.sopilnyak.test.service.command.factory.education;

import oleg.sopilnyak.test.service.command.factory.AbstractCommandFactory;
import oleg.sopilnyak.test.service.command.factory.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.education.base.EducationCommand;

import java.util.Collection;

/**
 * Commands factory for education syb-system
 */
public abstract class EducationCommandsFactory<T extends EducationCommand<?>>
        extends AbstractCommandFactory<T>
        implements CommandsFactory<T> {
    protected EducationCommandsFactory(Collection<T> commands) {
        super.applyFactoryCommands(commands);
    }
}
