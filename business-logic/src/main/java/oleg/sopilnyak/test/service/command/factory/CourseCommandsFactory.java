package oleg.sopilnyak.test.service.command.factory;

import oleg.sopilnyak.test.service.CommandsFactory;
import oleg.sopilnyak.test.service.command.course.CourseCommand;

import java.util.Collection;

/**
 * Commands factory for courses syb-system
 */
public class CourseCommandsFactory extends AbstractCommandFactory implements CommandsFactory {
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
