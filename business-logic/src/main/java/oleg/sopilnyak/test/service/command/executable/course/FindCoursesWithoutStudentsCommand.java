package oleg.sopilnyak.test.service.command.executable.course;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.RegisterPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.id.set.CourseCommands;

import java.util.Optional;
import java.util.Set;

/**
 * Command-Implementation: command to find courses without students
 */
@Slf4j
@AllArgsConstructor
public class FindCoursesWithoutStudentsCommand implements CourseCommand<Set<Course>> {
    private final RegisterPersistenceFacade persistenceFacade;

    /**
     * To find courses without students
     *
     * @param parameter not used
     * @return execution's result
     */
    @Override
    public CommandResult<Set<Course>> execute(Object parameter) {
        try {
            log.debug("Trying to find courses without students");
            final Set<Course> courses = persistenceFacade.findCoursesWithoutStudents();
            log.debug("Got courses without student {}", courses);
            return CommandResult.<Set<Course>>builder().success(true).result(Optional.of(courses)).build();
        } catch (Exception e) {
            log.error("Cannot find courses without students.", e);
            return CommandResult.<Set<Course>>builder().success(false).exception(e).result(Optional.of(Set.of())).build();
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return CourseCommands.FIND_NOT_REGISTERED.id();
    }
}
