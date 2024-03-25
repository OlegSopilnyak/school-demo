package oleg.sopilnyak.test.service.command.executable.course;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.id.set.CourseCommands;

import java.util.Optional;

/**
 * Command-Implementation: command to get course by id
 */
@Slf4j
@AllArgsConstructor
public class FindCourseCommand implements CourseCommand<Optional<Course>> {
    private final CoursesPersistenceFacade persistenceFacade;

    /**
     * To find course by id
     *
     * @param parameter system course-id
     * @return execution's result
     */
    @Override
    public CommandResult<Optional<Course>> execute(Object parameter) {
        try {
            log.debug("Trying to find course by ID:{}", parameter);
            final Long id = commandParameter(parameter);
            final Optional<Course> course = persistenceFacade.findCourseById(id);
            log.debug("Got course {} by ID:{}", course, id);
            return CommandResult.<Optional<Course>>builder().success(true).result(Optional.of(course)).build();
        } catch (Exception e) {
            log.error("Cannot find the course by ID:{}", parameter, e);
            return CommandResult.<Optional<Course>>builder().success(false).exception(e)
                    .result(Optional.of(Optional.empty())).build();
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return CourseCommands.FIND_BY_ID.id();
    }
}
