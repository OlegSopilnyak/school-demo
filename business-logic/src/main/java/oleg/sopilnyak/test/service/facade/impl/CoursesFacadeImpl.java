package oleg.sopilnyak.test.service.facade.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.*;
import oleg.sopilnyak.test.school.common.facade.CoursesFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

import java.util.Optional;
import java.util.Set;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.*;
import static oleg.sopilnyak.test.service.command.type.CourseCommand.*;

/**
 * Service: To process command for school's courses facade
 */
@Slf4j
@AllArgsConstructor
public class CoursesFacadeImpl<T> implements CoursesFacade {
    public static final String SOMETHING_WENT_WRONG = "Something went wrong";
    public static final String WRONG_COMMAND_EXECUTION = "For command-id:'{}' there is not exception after wrong command execution.";
    public static final String EXCEPTION_IS_NOT_STORED = "Exception is not stored!!!";
    private final CommandsFactory<T> factory;

    /**
     * To get the course by ID
     *
     * @param courseId system-id of the course
     * @return student instance or empty() if not exists
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Course> findById(Long courseId) {
        return executeSimpleCommand(FIND_BY_ID_COMMAND_ID, courseId, factory);
    }

    /**
     * To get courses registered for the student
     *
     * @param studentId system-id of the student
     * @return set of courses
     */
    @Override
    public Set<Course> findRegisteredFor(Long studentId) {
        return executeSimpleCommand(FIND_REGISTERED_COMMAND_ID, studentId, factory);
    }

    /**
     * To get courses without registered students
     *
     * @return set of courses
     */
    @Override
    public Set<Course> findWithoutStudents() {
        return executeSimpleCommand(FIND_NOT_REGISTERED_COMMAND_ID, null, factory);
    }

    /**
     * To create or update course instance
     *
     * @param course course should be created or updated
     * @return student instance or empty() if not exists
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Course> createOrUpdate(Course course) {
        return executeSimpleCommand(CREATE_OR_UPDATE_COMMAND_ID, course, factory);
    }

    /**
     * To delete course from the school
     *
     * @param courseId system-id of the course to delete
     * @throws CourseNotExistsException    throws when course it not exists
     * @throws CourseWithStudentsException throws when course is not empty (has registered students)
     */
    @Override
    public void delete(Long courseId) throws CourseNotExistsException, CourseWithStudentsException {
        final String commandId = DELETE_COMMAND_ID;
        final SchoolCommand<Boolean> command = takeValidCommand(commandId, factory);
        final CommandResult<Boolean> commandExecutionResult = command.execute(courseId);
        if (!commandExecutionResult.isSuccess()) {
            final Exception executionException = commandExecutionResult.getException();
            log.warn(SOMETHING_WENT_WRONG, executionException);
            if (executionException instanceof CourseNotExistsException exception) {
                throw exception;
            } else if (executionException instanceof CourseWithStudentsException exception) {
                throw exception;
            } else if (nonNull(executionException)) {
                throwFor(commandId, executionException);
            } else {
                log.error(WRONG_COMMAND_EXECUTION, commandId);
                throwFor(commandId, new NullPointerException(EXCEPTION_IS_NOT_STORED));
            }
        }
    }

    /**
     * To register the student to the school course
     *
     * @param studentId system-id of the student
     * @param courseId  system-id of the course
     * @throws StudentNotExistsException     throws when student is not exists
     * @throws CourseNotExistsException      throws if course is not exists
     * @throws NoRoomInTheCourseException    throws when there is no free slots for student
     * @throws StudentCoursesExceedException throws when student already registered to a lot ot courses
     */
    @Override
    public void register(Long studentId, Long courseId)
            throws StudentNotExistsException, CourseNotExistsException,
            NoRoomInTheCourseException, StudentCoursesExceedException {
        final String commandId = REGISTER_COMMAND_ID;
        final SchoolCommand<Boolean> command = takeValidCommand(commandId, factory);
        final CommandResult<Boolean> commandExecutionResult = command.execute(new Long[]{studentId, courseId});
        if (!commandExecutionResult.isSuccess()) {
            final Exception executionException = commandExecutionResult.getException();
            log.warn(SOMETHING_WENT_WRONG, executionException);
            if (executionException instanceof StudentNotExistsException exception) {
                throw exception;
            } else if (executionException instanceof CourseNotExistsException exception) {
                throw exception;
            } else if (executionException instanceof NoRoomInTheCourseException exception) {
                throw exception;
            } else if (executionException instanceof StudentCoursesExceedException exception) {
                throw exception;
            } else if (nonNull(executionException)) {
                throwFor(commandId, executionException);
            } else {
                log.error(WRONG_COMMAND_EXECUTION, commandId);
                throwFor(commandId, new NullPointerException(EXCEPTION_IS_NOT_STORED));
            }
        }
    }

    /**
     * To un-register the student from the school course
     *
     * @param studentId system-id of the student
     * @param courseId  system-id of the course
     * @throws StudentNotExistsException throws when student is not exists
     * @throws CourseNotExistsException  throws if course is not exists
     */
    @Override
    public void unRegister(Long studentId, Long courseId) throws StudentNotExistsException, CourseNotExistsException {
        final String commandId = UN_REGISTER_COMMAND_ID;
        final SchoolCommand<Boolean> command = takeValidCommand(commandId, factory);
        final CommandResult<Boolean> commandExecutionResult = command.execute(new Long[]{studentId, courseId});
        if (!commandExecutionResult.isSuccess()) {
            final Exception executionException = commandExecutionResult.getException();
            log.warn(SOMETHING_WENT_WRONG, executionException);
            if (executionException instanceof StudentNotExistsException exception) {
                throw exception;
            } else if (executionException instanceof CourseNotExistsException exception) {
                throw exception;
            } else if (nonNull(executionException)) {
                throwFor(commandId, executionException);
            } else {
                log.error(WRONG_COMMAND_EXECUTION, commandId);
                throwFor(commandId, new NullPointerException(EXCEPTION_IS_NOT_STORED));
            }
        }
    }
}
