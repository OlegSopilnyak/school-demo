package oleg.sopilnyak.test.service.command.course;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;

import java.util.Optional;

/**
 * Command-Implementation: command to get student by id
 */
@Slf4j
@AllArgsConstructor
public class FindCourseCommand implements SchoolCommand<Optional<Course>> {
    private final PersistenceFacade persistenceFacade;

    /**
     * To find student by id
     *
     * @param parameter system student-id
     * @return execution's result
     */
    @Override
    public CommandResult<Optional<Course>> execute(Object parameter) {
        try {
            log.debug("Trying to find course by ID:{}", parameter);
            Long id = (Long) parameter;
            Optional<Course> course = persistenceFacade.findCourseById(id);
            log.debug("Got student {} by ID:{}", course, id);
            return CommandResult.<Optional<Course>>builder()
                    .result(Optional.ofNullable(course))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find the student by ID:{}", parameter, e);
            return CommandResult.<Optional<Course>>builder().result(Optional.empty()).exception(e).success(false).build();
        }
    }
}
