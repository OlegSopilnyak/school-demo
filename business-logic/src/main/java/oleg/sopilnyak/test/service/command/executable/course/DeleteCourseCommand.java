package oleg.sopilnyak.test.service.command.executable.course;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.CourseNotExistsException;
import oleg.sopilnyak.test.school.common.exception.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.persistence.students.courses.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.ChangeCourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;
import org.springframework.util.ObjectUtils;

import java.util.Optional;

import static oleg.sopilnyak.test.school.common.persistence.students.courses.StudentsPersistenceFacade.isInvalidId;

/**
 * Command-Implementation: command to delete the course by id
 */
@Slf4j
@AllArgsConstructor
public class DeleteCourseCommand implements
        ChangeCourseCommand,
        CourseCommand<Boolean> {
    public static final String COURSE_WITH_ID_PREFIX = "Course with ID:";
    @Getter
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
                        .exception(new CourseNotExistsException(COURSE_WITH_ID_PREFIX + id + " is not exists."))
                        .build();
            }
            if (!ObjectUtils.isEmpty(course.get().getStudents())) {
                return CommandResult.<Boolean>builder().success(false).result(Optional.empty())
                        .exception(new CourseWithStudentsException(COURSE_WITH_ID_PREFIX + id + " has enrolled students."))
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
     * DO: To delete course by id<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context.State#WORK
     */
    @Override
    public void executeDo(Context<?> context) {
        final Object parameter = context.getRedoParameter();
        try {
            log.debug("Trying to delete course by ID: {}", parameter.toString());
            final Long inputId = commandParameter(parameter);
            if (isInvalidId(inputId)) {
                throw new CourseNotExistsException(COURSE_WITH_ID_PREFIX + inputId + " is not exists.");
            }
            final Course course = cacheEntityForRollback(inputId);
            if (!ObjectUtils.isEmpty(course.getStudents())) {
                throw new CourseWithStudentsException(COURSE_WITH_ID_PREFIX + inputId + " has enrolled students.");
            }
            // cached course saved to context for further rollback (undo)
            context.setUndoParameter(course);
            persistenceFacade.deleteCourse(inputId);
            context.setResult(true);
        } catch (Exception e) {
            log.error("Cannot save the student {}", parameter, e);
            context.failed(e);
            rollbackCachedEntity(context);
        }
    }

    /**
     * UNDO: To delete course by id<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     */
    @Override
    public void executeUndo(Context<?> context) {
        final Object parameter = context.getUndoParameter();
        try {
            log.debug("Trying to undo course deletion using: {}", parameter.toString());
            if (parameter instanceof Course course) {
                final Optional<Course> restored = persistenceFacade.save(course);
                log.debug("Got restored student {}", restored.orElse(null));
            } else {
                throw new CourseNotExistsException("Wrong undo parameter :" + parameter);
            }
            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo student deletion {}", parameter, e);
            context.failed(e);
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
