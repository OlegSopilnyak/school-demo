package oleg.sopilnyak.test.service.facade.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.exception.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.facade.StudentsFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.executable.CommandExecutor;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
import oleg.sopilnyak.test.service.command.id.set.StudentCommands;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

import java.util.Optional;
import java.util.Set;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.*;

/**
 * Service: To process command for school's student-facade
 */
@Slf4j
@AllArgsConstructor
public class StudentsFacadeImpl<T> implements StudentsFacade, StudentCommands {
    public static final String SOMETHING_WENT_WRONG = "Something went wrong";
    private final CommandsFactory<T> factory;

    /**
     * To get the student by ID
     *
     * @param studentId system-id of the student
     * @return student instance or empty() if not exists
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Student> findById(Long studentId) {
        return executeSimpleCommand(FIND_BY_ID, studentId, factory);
    }

    /**
     * To get students enrolled to the course
     *
     * @param courseId system-id of the course
     * @return set of students
     */
    @Override
    public Set<Student> findEnrolledTo(Long courseId) {
        return executeSimpleCommand(FIND_ENROLLED, courseId, factory);
    }

    /**
     * To get students not enrolled to any course
     *
     * @return set of students
     */
    @Override
    public Set<Student> findNotEnrolled() {
        return executeSimpleCommand(FIND_NOT_ENROLLED, null, factory);
    }

    /**
     * To create or update student instance
     *
     * @param student student should be created or updated
     * @return student instance or empty() if not exists
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Student> createOrUpdate(Student student) {
        return executeSimpleCommand(CREATE_OR_UPDATE, student, factory);
    }

    /**
     * To delete student from the school
     *
     * @param studentId system-id of the student
     * @return true if success
     * @throws StudentNotExistsException   throws when student it not exists
     * @throws StudentWithCoursesException throws when student is not empty (has enrolled courses)
     */
    @Override
    public boolean delete(Long studentId) throws StudentNotExistsException, StudentWithCoursesException {
        String commandId = DELETE;
        final SchoolCommand<Boolean> command = takeValidCommand(commandId, factory);
        final CommandResult<Boolean> cmdResult = command.execute(studentId);
        if (cmdResult.isSuccess()) {
            return cmdResult.getResult().orElseThrow(CommandExecutor.createThrowFor(commandId));
        } else {
            final Exception executionException = cmdResult.getException();
            log.warn(SOMETHING_WENT_WRONG, executionException);
            if (executionException instanceof StudentNotExistsException exception) {
                throw exception;
            } else if (executionException instanceof StudentWithCoursesException exception) {
                throw exception;
            } else if (nonNull(executionException)) {
                return throwFor(commandId, executionException);
            } else {
                log.error("For command-id:'{}' there is not exception after command execution.", commandId);
                return throwFor(commandId, new NullPointerException("Exception is not stored!!!"));
            }
        }
    }
}
