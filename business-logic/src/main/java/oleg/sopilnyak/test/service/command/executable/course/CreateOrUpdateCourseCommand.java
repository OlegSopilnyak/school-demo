package oleg.sopilnyak.test.service.command.executable.course;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.NotExistCourseException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.students.courses.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.command.SchoolCommandCache;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Command-Implementation: command to create or update the course
 */
@Slf4j
@Component
public class CreateOrUpdateCourseCommand
        extends SchoolCommandCache<Course>
        implements CourseCommand<Optional<Course>> {
    private final CoursesPersistenceFacade persistenceFacade;

    public CreateOrUpdateCourseCommand(CoursesPersistenceFacade persistenceFacade) {
        super(Course.class);
        this.persistenceFacade = persistenceFacade;
    }

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
            check(parameter);
            log.debug("Trying to create or update course {}", parameter);
            final Long inputId = ((Course) parameter).getId();
            final boolean isCreateCourse = PersistenceFacadeUtilities.isInvalidId(inputId);
            if (!isCreateCourse) {
                // cached course is storing to context for further rollback (undo)
                context.setUndoParameter(
                        retrieveEntity(inputId, persistenceFacade::findCourseById, persistenceFacade::toEntity,
                                () -> new NotExistCourseException(COURSE_WITH_ID_PREFIX + inputId + " is not exists.")
                        )
                );
            }
            final Optional<Course> course = persistRedoEntity(context, persistenceFacade::save);
            // checking execution context state
            afterPersistCheck(context, () -> rollbackCachedEntity(context, persistenceFacade::save), course, isCreateCourse);
        } catch (Exception e) {
            rollbackCachedEntity(context, persistenceFacade::save);
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

            rollbackCachedEntity(context, persistenceFacade::save, persistenceFacade::deleteCourse,
                    () -> new NotExistCourseException("Wrong undo parameter :" + parameter));

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
