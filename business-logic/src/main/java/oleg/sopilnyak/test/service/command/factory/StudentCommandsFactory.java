package oleg.sopilnyak.test.service.command.factory;

import oleg.sopilnyak.test.service.command.factory.base.AbstractCommandFactory;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;

import java.util.Collection;

/**
 * Commands factory for students syb-system
 */
public class StudentCommandsFactory extends AbstractCommandFactory<StudentCommand<?>> {
    public static final String NAME = "Students";

    public StudentCommandsFactory(Collection<StudentCommand<?>> commands) {
        super.applyFactoryCommands(commands);
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
        return (Class<F>) StudentCommand.class;
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
