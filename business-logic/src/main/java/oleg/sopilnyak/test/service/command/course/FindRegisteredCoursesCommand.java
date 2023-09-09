package oleg.sopilnyak.test.service.command.course;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.RegisterPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;

import java.util.Optional;
import java.util.Set;

/**
 * Command-Implementation: command to get students registered to the course
 */
@Slf4j
@AllArgsConstructor
public class FindRegisteredCoursesCommand implements SchoolCommand<Set<Course>> {
    private final RegisterPersistenceFacade persistenceFacade;

    /**
     * To find student by id
     *
     * @param parameter system student-id
     * @return execution's result
     */
    @Override
    public CommandResult<Set<Course>> execute(Object parameter) {
        try {
            log.debug("Trying to find courses registered to student: {}", parameter);
            Long id = (Long) parameter;
            Set<Course> courses = persistenceFacade.findCoursesRegisteredForStudent(id);
            log.debug("Got courses {} for student:{}", courses, id);
            return CommandResult.<Set<Course>>builder()
                    .result(Optional.ofNullable(courses))
                    .success(true)
                    .build();
        } catch (Exception e) {
            log.error("Cannot find the student by ID:{}", parameter, e);
            return CommandResult.<Set<Course>>builder().result(Optional.empty()).exception(e).success(false).build();
        }
    }
}
