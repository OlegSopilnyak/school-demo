package oleg.sopilnyak.test.service.command.factory;

import lombok.RequiredArgsConstructor;
import oleg.sopilnyak.test.service.CommandsFactory;
import oleg.sopilnyak.test.service.command.SchoolCommand;
import oleg.sopilnyak.test.service.command.course.CourseCommand;

import java.util.List;

import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * Commands factory for courses syb-system
 */
@RequiredArgsConstructor
public class CourseCommandsFactory implements CommandsFactory {
    private final List<CourseCommand> commands;

    /**
     * To get command instance by commandId
     *
     * @param commandId command-id
     * @return command instance
     */
    @Override
    public <T> SchoolCommand<T> command(String commandId) {
        return isEmpty(commandId) || isEmpty(commands) ? null :
                commands.stream().filter(cmd -> cmd.getId().equals(commandId)).findFirst().orElse(null);
    }

    /**
     * To get the name of the commands factory
     *
     * @return value
     */
    @Override
    public String getName() {
        return "courses";
    }

}
