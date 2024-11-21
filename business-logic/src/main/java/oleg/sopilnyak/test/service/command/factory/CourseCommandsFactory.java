package oleg.sopilnyak.test.service.command.factory;

import oleg.sopilnyak.test.service.command.factory.base.AbstractCommandFactory;
import oleg.sopilnyak.test.service.command.type.CourseCommand;

import java.util.Collection;

/**
 * Commands factory for courses syb-system
 */
public class CourseCommandsFactory extends AbstractCommandFactory<CourseCommand<?>> {
    public static final String NAME = "Courses";

    public CourseCommandsFactory(Collection<CourseCommand<?>> commands) {
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
