package oleg.sopilnyak.test.service.facade.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oleg.sopilnyak.test.school.common.business.StudentsFacade;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.exception.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.StudentPayload;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static oleg.sopilnyak.test.service.command.executable.CommandExecutor.*;
import static oleg.sopilnyak.test.service.command.type.StudentCommand.*;

/**
 * Service: To process command for school's student-facade
 */
@Slf4j
@AllArgsConstructor
public class StudentsFacadeImpl implements StudentsFacade {
    private final CommandsFactory<StudentCommand> factory;
    private final BusinessMessagePayloadMapper payloadMapper;

    /**
     * To get the student by ID
     *
     * @param id system-id of the student
     * @return student instance or empty() if not exists
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Student> findById(Long id) {
        log.debug("Find student by ID:{}", id);
        final String commandId = FIND_BY_ID_COMMAND_ID;
        final Optional<Student> result = doSimpleCommand(commandId, id, factory);
        log.debug("Found the student {}", result);
        return result.map(payloadMapper::toPayload);
    }

    /**
     * To get students enrolled to the course
     *
     * @param id system-id of the course
     * @return set of students
     */
    @Override
    public Set<Student> findEnrolledTo(Long id) {
        log.debug("Find students enrolled to the course with ID:{}", id);
        final String commandId = FIND_ENROLLED_COMMAND_ID;
        final Set<Student> result = doSimpleCommand(commandId, id, factory);
        log.debug("Found students enrolled to the course {}", result);
        return result.stream()
                .map(payloadMapper::toPayload).map(Student.class::cast)
                .collect(Collectors.toSet());
    }

    /**
     * To get students not enrolled to any course
     *
     * @return set of students
     */
    @Override
    public Set<Student> findNotEnrolled() {
        log.debug("Find students not enrolled to any course");
        final String commandId = FIND_NOT_ENROLLED_COMMAND_ID;
        final Set<Student> result = doSimpleCommand(commandId, null, factory);
        log.debug("Found students not enrolled to any course {}", result);
        return result.stream()
                .map(payloadMapper::toPayload).map(Student.class::cast)
                .collect(Collectors.toSet());
    }

    /**
     * To create or update student instance
     *
     * @param instance student should be created or updated
     * @return student instance or empty() if not exists
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Student> createOrUpdate(Student instance) {
        log.debug("Create or Update student {}", instance);
        final String commandId = CREATE_OR_UPDATE_COMMAND_ID;
        final StudentPayload payload = payloadMapper.toPayload(instance);
        final Optional<Student> result = doSimpleCommand(commandId, payload, factory);
        log.debug("Changed student {}", result);
        return result.map(payloadMapper::toPayload);
    }

    /**
     * To delete student from the school
     *
     * @param id system-id of the student
     * @return true if success
     * @throws NotExistStudentException    throws when student it not exists
     * @throws StudentWithCoursesException throws when student is not empty (has enrolled courses)
     */
    @Override
    public boolean delete(Long id) throws NotExistStudentException, StudentWithCoursesException {
        log.debug("Delete student with ID:{}", id);
        final String commandId = DELETE_COMMAND_ID;
        final SchoolCommand command = takeValidCommand(commandId, factory);
        final Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        if (context.isDone()) {
            log.debug("Deleted student with ID:{} successfully.", id);
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
