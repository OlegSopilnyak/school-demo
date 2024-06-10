package oleg.sopilnyak.test.service.facade.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.CoursesFacade;
import oleg.sopilnyak.test.school.common.exception.*;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.CoursePayload;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.*;
import static oleg.sopilnyak.test.service.command.type.CourseCommand.*;

/**
 * Service: To process command for school's courses facade
 */
@Slf4j
@AllArgsConstructor
public class CoursesFacadeImpl implements CoursesFacade {
    public static final String SOMETHING_WENT_WRONG = "Something went wrong";
    public static final String WRONG_COMMAND_EXECUTION = "For command-id:'{}' there is not exception after wrong command execution.";
    public static final String EXCEPTION_WAS_NOT_STORED = "Command fail Exception was not stored!!!";
    private final CommandsFactory<CourseCommand> factory;
    private final BusinessMessagePayloadMapper payloadMapper;

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
        log.debug("Find course by ID:{}", id);
        final Optional<Course> result = doSimpleCommand(FIND_BY_ID_COMMAND_ID, id, factory);
        log.debug("Found the course {}", result);
        return result.map(payloadMapper::toPayload);
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
        final Set<Course> result = doSimpleCommand(FIND_REGISTERED_COMMAND_ID, id, factory);
        log.debug("Found courses registered to student {}", result);
        return result.stream()
                .map(payloadMapper::toPayload).map(Course.class::cast)
                .collect(Collectors.toSet());
    }

    /**
     * To get courses without registered students
     *
     * @return set of courses
     */
    @Override
    public Set<Course> findWithoutStudents() {
        log.debug("Find no-students courses");
        final Set<Course> result = doSimpleCommand(FIND_NOT_REGISTERED_COMMAND_ID, null, factory);
        log.debug("Found no-students courses {}", result);
        return result.stream()
                .map(payloadMapper::toPayload).map(Course.class::cast)
                .collect(Collectors.toSet());
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
        final CoursePayload payload = payloadMapper.toPayload(instance);
        final Optional<Course> result = doSimpleCommand(CREATE_OR_UPDATE_COMMAND_ID, payload, factory);
        log.debug("Changed course {}", result);
        return result.map(payloadMapper::toPayload);
    }

    /**
     * To delete course from the school
     *
     * @param id system-id of the course to delete
     * @throws NotExistCourseException     throws when course it not exists
     * @throws CourseWithStudentsException throws when course is not empty (has registered students)
     */
    @Override
    public void delete(Long id) throws NotExistCourseException, CourseWithStudentsException {
        log.debug("Delete course with ID:{}", id);
        final String commandId = DELETE_COMMAND_ID;
        final SchoolCommand command = takeValidCommand(commandId, factory);
        final Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        if (context.isDone()) {
            log.debug("Deleted course with ID:{} successfully.", id);
            return;
        }

        final Exception doException = context.getException();
        log.warn(SOMETHING_WENT_WRONG, doException);
        if (doException instanceof NotExistCourseException exception) {
            throw exception;
        } else if (doException instanceof CourseWithStudentsException exception) {
            throw exception;
        } else if (nonNull(doException)) {
            throwFor(commandId, doException);
        } else {
            log.error(WRONG_COMMAND_EXECUTION, commandId);
            throwFor(commandId, new NullPointerException(EXCEPTION_WAS_NOT_STORED));
        }
    }

    /**
     * To register the student to the school course
     *
     * @param studentId system-id of the student
     * @param courseId  system-id of the course
     * @throws NotExistStudentException      throws when student is not exists
     * @throws NotExistCourseException       throws if course is not exists
     * @throws NoRoomInTheCourseException    throws when there is no free slots for student
     * @throws StudentCoursesExceedException throws when student already registered to a lot ot courses
     */
    @Override
    public void register(Long studentId, Long courseId)
            throws NotExistStudentException, NotExistCourseException,
            NoRoomInTheCourseException, StudentCoursesExceedException {
        log.debug("Register the student with ID:{} to the course with ID:{}", studentId, courseId);
        final String commandId = REGISTER_COMMAND_ID;
        final SchoolCommand command = takeValidCommand(commandId, factory);
        final Context<Boolean> context = command.createContext(new Long[]{studentId, courseId});

        command.doCommand(context);

        if (context.isDone()) {
            log.debug("Linked course:{} to student:{} successfully.", courseId, studentId);
            return;
        }

        final Exception doException = context.getException();
        log.warn(SOMETHING_WENT_WRONG, doException);
        if (doException instanceof NotExistStudentException exception) {
            throw exception;
        } else if (doException instanceof NotExistCourseException exception) {
            throw exception;
        } else if (doException instanceof NoRoomInTheCourseException exception) {
            throw exception;
        } else if (doException instanceof StudentCoursesExceedException exception) {
            throw exception;
        } else if (nonNull(doException)) {
            throwFor(commandId, doException);
        } else {
            log.error(WRONG_COMMAND_EXECUTION, commandId);
            throwFor(commandId, new NullPointerException(EXCEPTION_WAS_NOT_STORED));
        }
    }

    /**
     * To un-register the student from the school course
     *
     * @param studentId system-id of the student
     * @param courseId  system-id of the course
     * @throws NotExistStudentException throws when student is not exists
     * @throws NotExistCourseException  throws if course is not exists
     */
    @Override
    public void unRegister(Long studentId, Long courseId) throws NotExistStudentException, NotExistCourseException {
        log.debug("UnRegister the student with ID:{} from the course with ID:{}", studentId, courseId);
        final String commandId = UN_REGISTER_COMMAND_ID;
        final SchoolCommand command = takeValidCommand(commandId, factory);
        final Context<Boolean> context = command.createContext(new Long[]{studentId, courseId});

        command.doCommand(context);

        if (context.isDone()) {
            log.debug("Unlinked course:{} from student:{} successfully.", courseId, studentId);
            return;
        }

        final Exception doException = context.getException();
        log.warn(SOMETHING_WENT_WRONG, doException);
        if (doException instanceof NotExistStudentException exception) {
            throw exception;
        } else if (doException instanceof NotExistCourseException exception) {
            throw exception;
        } else if (nonNull(doException)) {
            throwFor(commandId, doException);
        } else {
            log.error(WRONG_COMMAND_EXECUTION, commandId);
            throwFor(commandId, new NullPointerException(EXCEPTION_WAS_NOT_STORED));
        }
    }
}
