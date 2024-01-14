package oleg.sopilnyak.test.service.command.factory;

import oleg.sopilnyak.test.service.command.factory.base.AbstractCommandFactory;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.StudentCommand;

import java.util.Collection;

/**
 * Commands factory for students syb-system
 */
public class StudentCommandsFactory<T> extends AbstractCommandFactory<T> implements CommandsFactory<T> {
    public static final String NAME = "Students";

    public StudentCommandsFactory(Collection<StudentCommand<T>> commands) {
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
