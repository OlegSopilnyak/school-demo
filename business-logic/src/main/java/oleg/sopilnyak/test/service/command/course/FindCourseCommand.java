package oleg.sopilnyak.test.service.command.course;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;
import oleg.sopilnyak.test.service.facade.course.CourseCommandsFacade;

import java.util.Optional;

/**
 * Command-Implementation: command to get course by id
 */
@Slf4j
@AllArgsConstructor
public class FindCourseCommand implements SchoolCommand<Optional<Course>> {
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
            Long id = (Long) parameter;
            Optional<Course> course = persistenceFacade.findCourseById(id);
            log.debug("Got course {} by ID:{}", course, id);
            return CommandResult.<Optional<Course>>builder()
                    .result(Optional.ofNullable(course))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find the course by ID:{}", parameter, e);
            return CommandResult.<Optional<Course>>builder()
                    .result(Optional.of(Optional.empty())).exception(e).success(false).build();
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return CourseCommandsFacade.FIND_BY_ID;
    }
}
