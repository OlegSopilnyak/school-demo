package oleg.sopilnyak.test.service.command.executable.course;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.ChangeCourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.slf4j.Logger;

import java.util.Optional;

import static oleg.sopilnyak.test.school.common.facade.peristence.students.courses.StudentsPersistenceFacade.isInvalidId;

/**
 * Command-Implementation: command to update the course
 */
@Slf4j
@AllArgsConstructor
public class CreateOrUpdateCourseCommand implements
        ChangeCourseCommand,
        CourseCommand<Optional<Course>> {
    @Getter
    private final CoursesPersistenceFacade persistenceFacade;

    /**
     * To create or update the course
     *
     * @param parameter student instance
     * @return execution's result
     * @deprecated commands are going to work through redo/undo
     */
    @Deprecated(forRemoval = true)
    @Override
    public CommandResult<Optional<Course>> execute(Object parameter) {
        try {
            log.debug("Trying to create or update course {}", parameter);
            final Course updated = commandParameter(parameter);
            final Optional<Course> course = persistenceFacade.save(updated);
            log.debug("Got stored course {} from parameter {}", course, updated);
            return CommandResult.<Optional<Course>>builder().success(true).result(Optional.of(course)).build();
        } catch (Exception e) {
            log.error("Cannot create or update course by ID:{}", parameter, e);
            return CommandResult.<Optional<Course>>builder().success(false).exception(e)
                    .result(Optional.of(Optional.empty())).build();
        }
    }

    /**
     * DO: To create or update the course<BR/>
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
            log.debug("Trying to create or update course {}", parameter);
            final Long inputId = ((Course) parameter).getId();
            final boolean isCreateCourse = isInvalidId(inputId);
            if (!isCreateCourse) {
                context.setUndoParameter(cacheEntityForRollback(inputId));
            }
            final Optional<Course> course = persistRedoEntity(context);
            // checking execution context state
            if (context.isFailed()) {
                // there was a fail during save course
                log.error("Cannot save course {}", parameter);
                rollbackCachedEntity(context);
            } else {
                // save course operation is done successfully
                log.debug("Got saved \ncourse {}\n for input {}", course, parameter);
                context.setResult(course);

                if (course.isPresent() && isCreateCourse) {
                    // saving created course.id for undo operation
                    context.setUndoParameter(course.get().getId());
                }
            }
        } catch (Exception e) {
            log.error("Cannot create or update course by ID:{}", parameter, e);
            context.failed(e);
        }
    }

    /**
     * UNDO: To create or update the course<BR/>
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
            log.debug("Trying to undo course changes using: {}", parameter.toString());
            if (parameter instanceof Long id) {
                persistenceFacade.deleteCourse(id);
                log.debug("Got deleted \ncourse ID:{}\n success: {}", id, true);
            } else if (parameter instanceof Course course) {
                persistenceFacade.save(course);
                log.debug("Got restored \ncourse {}\n success: {}", course, true);
            } else {
                throw new NullPointerException("Wrong undo parameter :" + parameter);
            }
            context.setState(Context.State.UNDONE);
        } catch (Exception e) {
            log.error("Cannot undo course change {}", parameter, e);
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
        return CREATE_OR_UPDATE_COMMAND_ID;
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
