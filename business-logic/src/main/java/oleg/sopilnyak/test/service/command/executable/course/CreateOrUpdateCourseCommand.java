package oleg.sopilnyak.test.service.command.executable.course;

import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.NotExistCourseException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.students.courses.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.utility.PersistenceFacadeUtilities;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.executable.cache.SchoolCommandCache;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Command-Implementation: command to create or update the course
 *
 * @see Course
 * @see CourseCommand
 * @see CoursesPersistenceFacade
 * @see SchoolCommandCache
 */
@Slf4j
@Component
public class CreateOrUpdateCourseCommand
        extends SchoolCommandCache<Course>
        implements CourseCommand {
    private final CoursesPersistenceFacade persistenceFacade;

    public CreateOrUpdateCourseCommand(CoursesPersistenceFacade persistenceFacade) {
        super(Course.class);
        this.persistenceFacade = persistenceFacade;
    }

    /**
     * DO: To create or update the course<BR/>
     * To execute command redo with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context.State#WORK
     * @see SchoolCommandCache#retrieveEntity(Long, LongFunction, UnaryOperator, Supplier)
     * @see SchoolCommandCache#persistRedoEntity(Context, Function)
     * @see SchoolCommandCache#rollbackCachedEntity(Context, Function)
     * @see CoursesPersistenceFacade#findCourseById(Long)
     * @see CoursesPersistenceFacade#toEntity(Course)
     * @see CoursesPersistenceFacade#save(Course)
     * @see NotExistCourseException
     */
    @Override
    public <T>void executeDo(Context<T> context) {
        final Object parameter = context.getRedoParameter();
        try {
            check(parameter);
            log.debug("Trying to create or update course {}", parameter);
            final Long id = ((Course) parameter).getId();
            final boolean isCreateCourse = PersistenceFacadeUtilities.isInvalidId(id);
            if (!isCreateCourse) {
                // previous version of course is storing to context for further rollback (undo)
                final Course previous = retrieveEntity(id,
                        persistenceFacade::findCourseById, persistenceFacade::toEntity,
                        () -> new NotExistCourseException(COURSE_WITH_ID_PREFIX + id + " is not exists.")
                );
                context.setUndoParameter(previous);
            }
            final Optional<Course> course = persistRedoEntity(context, persistenceFacade::save);
            // checking execution context state
            afterPersistCheck(context, () -> rollbackCachedEntity(context, persistenceFacade::save), course, isCreateCourse);
        } catch (Exception e) {
            log.error("Cannot create or update course by ID:{}", parameter, e);
            context.failed(e);
            rollbackCachedEntity(context, persistenceFacade::save);
        }
    }

    /**
     * UNDO: To create or update the course<BR/>
     * To rollback command's execution with correct context state
     *
     * @param context context of redo execution
     * @see Context
     * @see Context#getUndoParameter()
     * @see CoursesPersistenceFacade#save(Course)
     * @see CoursesPersistenceFacade#deleteCourse(Long)
     * @see NotExistCourseException
     */
    @Override
    public <T>void executeUndo(Context<T> context) {
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
