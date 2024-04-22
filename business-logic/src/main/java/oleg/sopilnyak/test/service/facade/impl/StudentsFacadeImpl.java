package oleg.sopilnyak.test.service.facade.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.exception.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.business.StudentsFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;

import java.util.Optional;
import java.util.Set;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.*;
import static oleg.sopilnyak.test.service.command.type.StudentCommand.*;

/**
 * Service: To process command for school's student-facade
 */
@Slf4j
@AllArgsConstructor
public class StudentsFacadeImpl<T> implements StudentsFacade {
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
        return doSimpleCommand(FIND_BY_ID_COMMAND_ID, studentId, factory);
    }

    /**
     * To get students enrolled to the course
     *
     * @param courseId system-id of the course
     * @return set of students
     */
    @Override
    public Set<Student> findEnrolledTo(Long courseId) {
        return doSimpleCommand(FIND_ENROLLED_COMMAND_ID, courseId, factory);
    }

    /**
     * To get students not enrolled to any course
     *
     * @return set of students
     */
    @Override
    public Set<Student> findNotEnrolled() {
        return doSimpleCommand(FIND_NOT_ENROLLED_COMMAND_ID, null, factory);
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
        return doSimpleCommand(CREATE_OR_UPDATE_COMMAND_ID, student, factory);
    }

    /**
     * To delete student from the school
     *
     * @param studentId system-id of the student
     * @return true if success
     * @throws NotExistStudentException   throws when student it not exists
     * @throws StudentWithCoursesException throws when student is not empty (has enrolled courses)
     */
    @Override
    public boolean delete(Long studentId) throws NotExistStudentException, StudentWithCoursesException {
        String commandId = DELETE_COMMAND_ID;
        final SchoolCommand<Boolean> command = takeValidCommand(commandId, factory);
        final Context<Boolean> context = command.createContext(studentId);

        command.doCommand(context);

        if (context.isDone()) {
            log.debug("Deleted student with ID:{} successfully.", studentId);
            return true;
        }

        final Exception doException = context.getException();
        log.warn("Something went wrong", doException);
        if (doException instanceof NotExistStudentException studentNotExistsException) {
            throw studentNotExistsException;
        } else if (doException instanceof StudentWithCoursesException studentWithCoursesException) {
            throw studentWithCoursesException;
        } else if (nonNull(doException)) {
            return throwFor(commandId, doException);
        } else {
            log.error("For command-id:'{}' there is not exception after command execution.", commandId);
            return throwFor(commandId, new NullPointerException("Exception is not stored!!!"));
        }
    }
}
