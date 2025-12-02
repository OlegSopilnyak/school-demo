package oleg.sopilnyak.test.service.facade.education.impl;

import static java.util.Objects.nonNull;

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
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand.CommandId;
import oleg.sopilnyak.test.service.facade.ActionFacade;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

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

    public CoursesFacadeImpl(
            CommandsFactory<CourseCommand<?>> factory,
            BusinessMessagePayloadMapper mapper,
            ActionExecutor actionExecutor
    ) {
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
        final Optional<Optional<Course>> result;
        result = actCommand(CommandId.FIND_BY_ID, factory, Input.of(id));
        if (result.isPresent()) {
            final Optional<Course> course = result.get();
            log.debug("Found the course {}", course);
            return course.map(toPayload);
        }
        return Optional.empty();
    }

    /**
     * To get courses registered for the student
     *
     * @param id system-id of the student
     * @return set of courses
     */
    @Override
    public Set<Course> findRegisteredFor(final Long id) {
        log.debug("Finding courses registered to student with ID:{}", id);
        final Optional<Set<Course>> result;
        result = actCommand(CommandId.FIND_REGISTERED, factory, Input.of(id));
        if (result.isPresent()) {
            final Set<Course> courses = result.get();
            log.debug("Found courses registered to student {}", courses);
            return courses.stream().map(toPayload).collect(Collectors.toSet());
        }
        return Set.of();
    }

    /**
     * To get courses without registered students
     *
     * @return set of courses
     */
    @Override
    public Set<Course> findWithoutStudents() {
        log.debug("Finding no-students courses");
        final Optional<Set<Course>> result;
        result = actCommand(CommandId.FIND_NOT_REGISTERED, factory, Input.empty());
        if (result.isPresent()) {
            final Set<Course> courses = result.get();
            log.debug("Found no-students courses {}", courses);
            return courses.stream().map(toPayload).collect(Collectors.toSet());
        }
        return Set.of();
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
        log.debug("Creating or Updating course {}", instance);
        final var input = Input.of(toPayload.apply(instance));
        final Optional<Optional<Course>> result;
        result = actCommand(CommandId.CREATE_OR_UPDATE, factory, input);
        if (result.isPresent()) {
            final Optional<Course> course = result.get();
            log.debug("Changed course {}", course);
            return course.map(toPayload);
        }
        return Optional.empty();
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
        final String commandId = CommandId.DELETE;
        final Consumer<Exception> doThisOnError = exception -> {
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
        log.debug("Deleting course with ID:{}", id);
        final Optional<Boolean> result = actCommand(commandId, factory, Input.of(id), doThisOnError);
        result.ifPresent(executionResult ->
                log.debug("Deleted course with ID:{} successfully:{} .", id, executionResult)
        );
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
        final String commandId = CommandId.REGISTER;
        final Consumer<Exception> doThisOnError = exception -> {
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
        log.debug("Registering the student with ID:{} to the course with ID:{}", studentId, courseId);
        final var input = Input.of(studentId, courseId);
        final Optional<Boolean> result = actCommand(commandId, factory, input, doThisOnError);
        result.ifPresent(executionResult ->
                log.debug("Linked course:{} with student:{} successfully:{}.", courseId, studentId, executionResult)
        );
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
        final String commandId = CommandId.UN_REGISTER;
        final Consumer<Exception> doThisOnError = exception -> {
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

        log.debug("UnRegistering the student with ID:{} from the course with ID:{}", studentId, courseId);
        final var input = Input.of(studentId, courseId);
        final Optional<Boolean> result = actCommand(commandId, factory, input, doThisOnError);
        result.ifPresent(executionResult ->
                log.debug("Unlinked course:{} from student:{} successfully:{} .", courseId, studentId, executionResult)
        );
    }

    /**
     * To get the logger of the facade
     *
     * @return logger instance
     */
    @Override
    public Logger getLogger() {
        return log;
    }
}
