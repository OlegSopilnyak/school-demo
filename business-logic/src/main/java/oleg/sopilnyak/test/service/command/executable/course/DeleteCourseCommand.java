package oleg.sopilnyak.test.service.command.executable.course;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.CourseNotExistsException;
import oleg.sopilnyak.test.school.common.exception.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import org.slf4j.Logger;
import org.springframework.util.ObjectUtils;

import java.util.Optional;

/**
 * Command-Implementation: command to delete the course by id
 */
@Slf4j
@AllArgsConstructor
public class DeleteCourseCommand implements CourseCommand<Boolean> {
    private final CoursesPersistenceFacade persistenceFacade;

    /**
     * To delete course by id
     *
     * @param parameter system course-id
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Boolean> execute(Object parameter) {
        try {
            log.debug("Trying to delete course: {}", parameter);
            final Long id = commandParameter(parameter);
            final Optional<Course> course = persistenceFacade.findCourseById(id);
            if (course.isEmpty()) {
                return CommandResult.<Boolean>builder().success(false).result(Optional.empty())
                        .exception(new CourseNotExistsException("Course with ID:" + id + " is not exists."))
                        .build();
            }
            if (!ObjectUtils.isEmpty(course.get().getStudents())) {
                return CommandResult.<Boolean>builder().success(false).result(Optional.empty())
                        .exception(new CourseWithStudentsException("Course with ID:" + id + " has enrolled students."))
                        .build();
            }
            final boolean success = persistenceFacade.deleteCourse(id);
            log.debug("Deleted course {} {}", course.get(), success);
            return CommandResult.<Boolean>builder().success(true).result(Optional.of(success)).build();
        } catch (Exception e) {
            log.error("Cannot delete the course by ID:{}", parameter, e);
            return CommandResult.<Boolean>builder().success(false).result(Optional.empty()).exception(e).build();
        }
    }

    /**
     * To get unique command-id for the command
     *
     * @return value of command-id
     */
    @Override
    public String getId() {
        return DELETE_COMMAND_ID;
    }

    /**
     * To get reference to command's logger
     *
     * @return reference to the logger
     */
    @Override
    public Logger getLog() {
        return log;
    }
}
