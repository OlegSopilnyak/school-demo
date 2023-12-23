package oleg.sopilnyak.test.service.command.course;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.RegisterPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.facade.course.CourseCommandsFacade;

import java.util.Optional;
import java.util.Set;

/**
 * Command-Implementation: command to find courses registered to student
 */
@Slf4j
@AllArgsConstructor
public class FindRegisteredCoursesCommand implements CourseCommand<Set<Course>> {
    private final RegisterPersistenceFacade persistenceFacade;

    /**
     * To find courses registered to student by id
     *
     * @param parameter system student-id
     * @return execution's result
     */
    @Override
    public CommandResult<Set<Course>> execute(Object parameter) {
        try {
            log.debug("Trying to find courses registered to student ID: {}", parameter);
            Long id = (Long) parameter;
            Set<Course> courses = persistenceFacade.findCoursesRegisteredForStudent(id);
            log.debug("Got courses {} for student ID:{}", courses, id);
            return CommandResult.<Set<Course>>builder()
                    .result(Optional.ofNullable(courses))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find courses registered to student ID:{}", parameter, e);
            return CommandResult.<Set<Course>>builder()
                    .result(Optional.of(Set.of())).exception(e).success(false).build();
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return CourseCommandsFacade.FIND_REGISTERED;
    }
}
