package oleg.sopilnyak.test.service.facade.education.impl;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.type.education.CourseCommand.CREATE_OR_UPDATE;
import static oleg.sopilnyak.test.service.command.type.education.CourseCommand.DELETE;
import static oleg.sopilnyak.test.service.command.type.education.CourseCommand.FIND_BY_ID;
import static oleg.sopilnyak.test.service.command.type.education.CourseCommand.FIND_NOT_REGISTERED;
import static oleg.sopilnyak.test.service.command.type.education.CourseCommand.FIND_REGISTERED;
import static oleg.sopilnyak.test.service.command.type.education.CourseCommand.REGISTER;
import static oleg.sopilnyak.test.service.command.type.education.CourseCommand.UN_REGISTER;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.facade.education.CoursesFacade;
import oleg.sopilnyak.test.school.common.exception.education.CourseHasNoRoomException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.exception.education.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.facade.ActionFacade;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;
import org.slf4j.Logger;

/**
 * Service: To process command for school's courses facade
 */
@Slf4j
public class CoursesFacadeImpl implements CoursesFacade, ActionFacade {
    private final CommandsFactory<? extends RootCommand<?>> factory;
    @Getter
    private final ActionExecutor actionExecutor;
    // semantic data to payload converter
    private final UnaryOperator<Course> toPayload;

    public CoursesFacadeImpl(CommandsFactory<CourseCommand<?>> factory, BusinessMessagePayloadMapper mapper, ActionExecutor actionExecutor) {
        this.factory = factory;
        this.actionExecutor = actionExecutor;
        this.toPayload = course -> course instanceof CoursePayload payload ? payload : mapper.toPayload(course);
    }

    /**
     * To get the course by ID
     *
     * @param id system-id of the course
     * @return course instance or empty() if not exists
     * @see Course
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Course> findById(final Long id) {
        log.debug("Finding course by ID: {}", id);
        final Optional<Course> result = actCommand(FIND_BY_ID, factory, Input.of(id));
        log.debug("Found the course {}", result);
        return result.map(toPayload);
    }

    /**
     * To get courses registered for the student
     *
     * @param id system-id of the student
     * @return set of courses
     */
    @Override
    public Set<Course> findRegisteredFor(final Long id) {
        log.debug("Find courses registered to student with ID:{}", id);
        final Set<Course> result = actCommand(FIND_REGISTERED, factory, Input.of(id));
        log.debug("Found courses registered to student {}", result);
        return result.stream().map(toPayload).collect(Collectors.toSet());
    }

    /**
     * To get courses without registered students
     *
     * @return set of courses
     */
    @Override
    public Set<Course> findWithoutStudents() {
        log.debug("Find no-students courses");
        final Set<Course> result = actCommand(FIND_NOT_REGISTERED, factory, Input.empty());
        log.debug("Found no-students courses {}", result);
        return result.stream().map(toPayload).collect(Collectors.toSet());
    }

    /**
     * To create or update course instance
     *
     * @param instance course should be created or updated
     * @return student instance or empty() if not exists
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Course> createOrUpdate(final Course instance) {
        log.debug("Create or Update course {}", instance);
        final Optional<Course> result = actCommand(CREATE_OR_UPDATE, factory, Input.of(toPayload.apply(instance)));
        log.debug("Changed course {}", result);
        return result.map(toPayload);
    }

    /**
     * To delete course from the school
     *
     * @param id system-id of the course to delete
     * @throws CourseNotFoundException     throws when course it not exists
     * @throws CourseWithStudentsException throws when course is not empty (has registered students)
     */
    @Override
    public void delete(Long id) throws CourseNotFoundException, CourseWithStudentsException {
        final String commandId = DELETE;
        final Consumer<Exception> onError = exception -> {
            logSomethingWentWrong(exception, commandId);
            if (exception instanceof CourseNotFoundException noCourseException) {
                throw noCourseException;
            } else if (exception instanceof CourseWithStudentsException notEmptyCourse) {
                throw notEmptyCourse;
            } else if (nonNull(exception)) {
                ActionFacade.throwFor(commandId, exception);
            } else {
                failedButNoExceptionStored(commandId);
            }
        };
        log.debug("Delete course with ID:{}", id);
        final Boolean result = actCommand(commandId, factory, Input.of(id), onError);
        log.debug("Deleted course with ID:{} successfully:{} .", id, result);
    }

    /**
     * To register the student to the school course
     *
     * @param studentId system-id of the student
     * @param courseId  system-id of the course
     * @throws StudentNotFoundException      throws when student is not exists
     * @throws CourseNotFoundException       throws if course is not exists
     * @throws CourseHasNoRoomException      throws when there is no free slots for student
     * @throws StudentCoursesExceedException throws when student already registered to a lot ot courses
     */
    @Override
    public void register(Long studentId, Long courseId)
            throws StudentNotFoundException, CourseNotFoundException,
            CourseHasNoRoomException, StudentCoursesExceedException {
        final String commandId = REGISTER;
        final Consumer<Exception> onError = exception -> {
            logSomethingWentWrong(exception, commandId);
            if (exception instanceof StudentNotFoundException noStudentException) {
                throw noStudentException;
            } else if (exception instanceof CourseNotFoundException noCourseException) {
                throw noCourseException;
            } else if (exception instanceof CourseHasNoRoomException noRoomException) {
                throw noRoomException;
            } else if (exception instanceof StudentCoursesExceedException coursesExceedException) {
                throw coursesExceedException;
            } else if (nonNull(exception)) {
                ActionFacade.throwFor(commandId, exception);
            } else {
                failedButNoExceptionStored(commandId);
            }
        };

        log.debug("Register the student with ID:{} to the course with ID:{}", studentId, courseId);
        final Boolean result = actCommand(commandId, factory, Input.of(studentId, courseId), onError);
        log.debug("Linked course:{} to student:{} successfully:{}.", courseId, studentId, result);
    }

    /**
     * To un-register the student from the school course
     *
     * @param studentId system-id of the student
     * @param courseId  system-id of the course
     * @throws StudentNotFoundException throws when student is not exists
     * @throws CourseNotFoundException  throws if course is not exists
     */
    @Override
    public void unRegister(Long studentId, Long courseId) throws StudentNotFoundException, CourseNotFoundException {
        final String commandId = UN_REGISTER;
        final Consumer<Exception> onError = exception -> {
            logSomethingWentWrong(exception, commandId);
            if (exception instanceof StudentNotFoundException noStudentException) {
                throw noStudentException;
            } else if (exception instanceof CourseNotFoundException noCourseException) {
                throw noCourseException;
            } else if (nonNull(exception)) {
                ActionFacade.throwFor(commandId, exception);
            } else {
                failedButNoExceptionStored(commandId);
            }
        };

        log.debug("UnRegister the student with ID:{} from the course with ID:{}", studentId, courseId);
        final Boolean result = actCommand(commandId, factory, Input.of(studentId, courseId), onError);
        log.debug("Unlinked course:{} from student:{} successfully:{} .", courseId, studentId, result);
    }


    @Override
    public Logger getLogger() {
        return log;
    }

    // private methods
}
