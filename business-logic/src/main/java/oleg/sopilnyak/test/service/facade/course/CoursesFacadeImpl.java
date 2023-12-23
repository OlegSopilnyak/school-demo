package oleg.sopilnyak.test.service.facade.course;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.*;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.CommandsFactory;
import oleg.sopilnyak.test.service.command.CommandExecutor;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;

import java.util.Optional;
import java.util.Set;

import static oleg.sopilnyak.test.service.command.CommandExecutor.executeSimpleCommand;
import static oleg.sopilnyak.test.service.command.CommandExecutor.takeValidCommand;

/**
 * Service: To process command for school's student-facade
 */
@Slf4j
@AllArgsConstructor
public class CoursesFacadeImpl implements CourseCommandsFacade {
    public static final String SOMETHING_WENT_WRONG = "Something went wrong";
    private final CommandsFactory factory;

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
        return CommandExecutor.executeSimpleCommand(FIND_BY_ID, courseId, factory);
    }

    /**
     * To get courses registered for the student
     *
     * @param studentId system-id of the student
     * @return set of courses
     */
    @Override
    public Set<Course> findRegisteredFor(Long studentId) {
        return executeSimpleCommand(FIND_REGISTERED, studentId, factory);
    }

    /**
     * To get courses without registered students
     *
     * @return set of courses
     */
    @Override
    public Set<Course> findWithoutStudents() {
        return executeSimpleCommand(FIND_NOT_REGISTERED, null, factory);
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
        return executeSimpleCommand(CREATE_OR_UPDATE, course, factory);
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
        String commandId = DELETE;
        final SchoolCommand<Boolean> command = takeValidCommand(commandId, factory);
        final CommandResult<Boolean> cmdResult = command.execute(courseId);
        if (!cmdResult.isSuccess()) {
            Exception executionException = cmdResult.getException();
            log.warn(SOMETHING_WENT_WRONG, executionException);
            if (executionException instanceof CourseNotExistsException exception) {
                throw exception;
            } else if (executionException instanceof CourseWithStudentsException exception) {
                throw exception;
            } else {
                CommandExecutor.throwFor(commandId, cmdResult.getException());
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
        String commandId = REGISTER;
        SchoolCommand<Boolean> command = factory.command(commandId);
        CommandResult<Boolean> cmdResult = command.execute(new Long[] {studentId, courseId});
        if (!cmdResult.isSuccess()) {
            Exception executionException = cmdResult.getException();
            log.warn(SOMETHING_WENT_WRONG, executionException);
            if (executionException instanceof StudentNotExistsException exception) {
                throw exception;
            } else if (executionException instanceof CourseNotExistsException exception) {
                throw exception;
            } else if (executionException instanceof NoRoomInTheCourseException exception) {
                throw exception;
            } else if (executionException instanceof StudentCoursesExceedException exception) {
                throw exception;
            } else {
                CommandExecutor.throwFor(commandId, cmdResult.getException());
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
        String commandId = UN_REGISTER;
        SchoolCommand<Boolean> command = factory.command(commandId);
        CommandResult<Boolean> cmdResult = command.execute(new Long[] {studentId, courseId});
        if (!cmdResult.isSuccess()) {
            Exception executionException = cmdResult.getException();
            log.warn(SOMETHING_WENT_WRONG, executionException);
            if (executionException instanceof StudentNotExistsException exception) {
                throw exception;
            } else if (executionException instanceof CourseNotExistsException exception) {
                throw exception;
            } else {
                CommandExecutor.throwFor(commandId, cmdResult.getException());
            }
        }
    }

}
