package oleg.sopilnyak.test.service.command.factory;

import oleg.sopilnyak.test.service.CommandsFactory;
import oleg.sopilnyak.test.service.command.course.CourseCommand;

import java.util.Collection;

/**
 * Commands factory for courses syb-system
 */
public class CourseCommandsFactory<T> extends AbstractCommandFactory<T> implements CommandsFactory<T> {
    public static final String NAME = "Courses";

    public CourseCommandsFactory(Collection<CourseCommand<T>> commands) {
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
