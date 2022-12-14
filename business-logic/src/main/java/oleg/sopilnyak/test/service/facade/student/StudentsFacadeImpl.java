package oleg.sopilnyak.test.service.facade.student;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.exception.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.CommandsFactory;
import oleg.sopilnyak.test.service.command.CommandExecutor;
import oleg.sopilnyak.test.service.command.CommandResult;
import oleg.sopilnyak.test.service.command.SchoolCommand;

import java.util.Optional;
import java.util.Set;

import static oleg.sopilnyak.test.service.command.CommandExecutor.executeSimpleCommand;

/**
 * Service: To process command for school's student-facade
 */
@Slf4j
@AllArgsConstructor
public class StudentsFacadeImpl implements StudentCommandFacade {
    private final CommandsFactory factory;

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
        SchoolCommand<Boolean> command = factory.command(commandId);
        CommandResult<Boolean> cmdResult = command.execute(studentId);
        if (cmdResult.isSuccess()) {
            return cmdResult.getResult().orElseThrow(CommandExecutor.throwFor(commandId));
        } else {
            Exception commandException = cmdResult.getException();
            if (commandException instanceof StudentNotExistsException) {
                throw (StudentNotExistsException) commandException;
            } else if (commandException instanceof StudentWithCoursesException) {
                throw (StudentWithCoursesException) commandException;
            } else {
                return CommandExecutor.throwFor(commandId, cmdResult.getException());
            }
        }
    }
}
