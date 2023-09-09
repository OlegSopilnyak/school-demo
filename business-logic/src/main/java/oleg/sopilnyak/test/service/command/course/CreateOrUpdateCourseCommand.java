package oleg.sopilnyak.test.service.command.course;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;

import java.util.Optional;

/**
 * Command-Implementation: command to update the course
 */
@Slf4j
@AllArgsConstructor
public class CreateOrUpdateCourseCommand implements SchoolCommand<Optional<Course>> {
    private final CoursesPersistenceFacade persistenceFacade;

    /**
     * To find student by id
     *
     * @param parameter system student-id
     * @return execution's result
     */
    @Override
    public CommandResult<Optional<Course>> execute(Object parameter) {
        try {
            log.debug("Trying to create or update course {}", parameter);
            Course updated = (Course) parameter;
            Optional<Course> course = persistenceFacade.save(updated);
            log.debug("Got stored student {} from parameter {}", course, updated);
            return CommandResult.<Optional<Course>>builder()
                    .result(Optional.ofNullable(course))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot create or update course by ID:{}", parameter, e);
            return CommandResult.<Optional<Course>>builder()
                    .result(Optional.of(Optional.empty()))
                    .exception(e).success(false).build();
        }
    }
}
