package oleg.sopilnyak.test.service.facade.impl;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.doSimpleCommand;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.takeValidCommand;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.throwFor;
import static oleg.sopilnyak.test.service.command.type.CourseCommand.CREATE_OR_UPDATE;
import static oleg.sopilnyak.test.service.command.type.CourseCommand.DELETE;
import static oleg.sopilnyak.test.service.command.type.CourseCommand.FIND_BY_ID;
import static oleg.sopilnyak.test.service.command.type.CourseCommand.FIND_NOT_REGISTERED;
import static oleg.sopilnyak.test.service.command.type.CourseCommand.FIND_REGISTERED;
import static oleg.sopilnyak.test.service.command.type.CourseCommand.REGISTER;
import static oleg.sopilnyak.test.service.command.type.CourseCommand.UN_REGISTER;

import java.util.Optional;
import java.util.Set;
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
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.facade.ActionExecutorFacade;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;
import org.slf4j.Logger;

/**
 * Service: To process command for school's courses facade
 */
@Slf4j
public class CoursesFacadeImpl implements CoursesFacade, ActionExecutorFacade {
    public static final String SOMETHING_WENT_WRONG = "Something went wrong";
    public static final String WRONG_COMMAND_EXECUTION = "For command-id:'{}' there is not exception after wrong command execution.";
    public static final String EXCEPTION_WAS_NOT_STORED = "Command fail Exception was not stored!!!";
    @Getter
    private final CommandsFactory<? extends RootCommand<?>> factory;
    // semantic data to payload converter
    private final UnaryOperator<Course> convert;

    public CoursesFacadeImpl(CommandsFactory<CourseCommand<?>> factory, BusinessMessagePayloadMapper mapper) {
        this.factory = factory;
        this.convert = course -> course instanceof CoursePayload payload? payload : mapper.toPayload(course);
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
    public Optional<Course> findById(Long id) {
        log.debug("Finding course by ID: {}", id);
        final Optional<Course> result = doActionCommand(FIND_BY_ID, Input.of(id));
        log.debug("Found the course {}", result);
        return result.map(convert);
    }

    /**
     * To get courses registered for the student
     *
     * @param id system-id of the student
     * @return set of courses
     */
    @Override
    public Set<Course> findRegisteredFor(Long id) {
        log.debug("Find courses registered to student with ID:{}", id);
        final Set<Course> result = (Set<Course>) doSimpleCommand(FIND_REGISTERED, Input.of(id), factory);
        log.debug("Found courses registered to student {}", result);
        return result.stream().map(convert).collect(Collectors.toSet());
    }

    /**
     * To get courses without registered students
     *
     * @return set of courses
     */
    @Override
    public Set<Course> findWithoutStudents() {
        log.debug("Find no-students courses");
        final Set<Course> result = (Set<Course>) doSimpleCommand(FIND_NOT_REGISTERED, null, factory);
        log.debug("Found no-students courses {}", result);
        return result.stream().map(convert).collect(Collectors.toSet());
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
    public Optional<Course> createOrUpdate(Course instance) {
        log.debug("Create or Update course {}", instance);
        final Optional<Course> result = (Optional<Course>) doSimpleCommand(CREATE_OR_UPDATE, Input.of(convert.apply(instance)), factory);
        log.debug("Changed course {}", result);
        return result.map(convert);
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
        log.debug("Delete course with ID:{}", id);
        final String commandId = DELETE;
        final RootCommand<Boolean> command = (RootCommand<Boolean>) takeValidCommand(commandId, factory);
        final Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        if (context.isDone()) {
            // success processing
            log.debug("Deleted course with ID:{} successfully.", id);
            return;
        }

        // fail processing
        final Exception deleteException = context.getException();
        log.warn(SOMETHING_WENT_WRONG, deleteException);
        if (deleteException instanceof CourseNotFoundException noCourseException) {
            throw noCourseException;
        } else if (deleteException instanceof CourseWithStudentsException exception) {
            throw exception;
        } else if (nonNull(deleteException)) {
            throwFor(commandId, deleteException);
        } else {
            wrongCommandExecution(commandId);
        }
    }

    /**
     * To register the student to the school course
     *
     * @param studentId system-id of the student
     * @param courseId  system-id of the course
     * @throws StudentNotFoundException      throws when student is not exists
     * @throws CourseNotFoundException       throws if course is not exists
     * @throws CourseHasNoRoomException    throws when there is no free slots for student
     * @throws StudentCoursesExceedException throws when student already registered to a lot ot courses
     */
    @Override
    public void register(Long studentId, Long courseId)
            throws StudentNotFoundException, CourseNotFoundException,
            CourseHasNoRoomException, StudentCoursesExceedException {
        log.debug("Register the student with ID:{} to the course with ID:{}", studentId, courseId);
        final String commandId = REGISTER;
        final RootCommand<Boolean> command = (RootCommand<Boolean>) takeValidCommand(commandId, factory);
        final Context<Boolean> context = command.createContext(Input.of(studentId, courseId));

        command.doCommand(context);

        if (context.isDone()) {
            // success processing
            log.debug("Linked course:{} to student:{} successfully.", courseId, studentId);
            return;
        }

        // fail processing
        final Exception registerException = context.getException();
        log.warn(SOMETHING_WENT_WRONG, registerException);
        if (registerException instanceof StudentNotFoundException noStudentException) {
            throw noStudentException;
        } else if (registerException instanceof CourseNotFoundException noCourseException) {
            throw noCourseException;
        } else if (registerException instanceof CourseHasNoRoomException noRoomException) {
            throw noRoomException;
        } else if (registerException instanceof StudentCoursesExceedException coursesExceedException) {
            throw coursesExceedException;
        } else if (nonNull(registerException)) {
            throwFor(commandId, registerException);
        } else {
            wrongCommandExecution(commandId);
        }
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
        log.debug("UnRegister the student with ID:{} from the course with ID:{}", studentId, courseId);
        final String commandId = UN_REGISTER;
        final RootCommand<Boolean> command = (RootCommand<Boolean>) takeValidCommand(commandId, factory);
        final Context<Boolean> context = command.createContext(Input.of(studentId, courseId));

        command.doCommand(context);

        if (context.isDone()) {
            // success processing
            log.debug("Unlinked course:{} from student:{} successfully.", courseId, studentId);
            return;
        }

        // fail processing
        final Exception unregisterException = context.getException();
        log.warn(SOMETHING_WENT_WRONG, unregisterException);
        if (unregisterException instanceof StudentNotFoundException noStudentException) {
            throw noStudentException;
        } else if (unregisterException instanceof CourseNotFoundException noCourseException) {
            throw noCourseException;
        } else if (nonNull(unregisterException)) {
            throwFor(commandId, unregisterException);
        } else {
            wrongCommandExecution(commandId);
        }
    }

    @Override
    public Logger getLogger() {
        return log;
    }

    // private methods
    private static void wrongCommandExecution(String commandId) {
        log.error(WRONG_COMMAND_EXECUTION, commandId);
        throwFor(commandId, new NullPointerException(EXCEPTION_WAS_NOT_STORED));
    }
}
