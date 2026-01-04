package oleg.sopilnyak.test.service.facade.education.impl;

import static oleg.sopilnyak.test.service.command.type.education.StudentCommand.CommandId;

import oleg.sopilnyak.test.school.common.business.facade.education.StudentsFacade;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.facade.ActionFacade;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;

import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Service: To process command for school's student-facade
 */
@Slf4j
public class StudentsFacadeImpl implements StudentsFacade, ActionFacade {
    private final CommandsFactory<StudentCommand<?>> factory;
    @Getter
    private final CommandActionExecutor actionExecutor;
    // semantic data to payload converter
    private final UnaryOperator<Student> toPayload;

    public StudentsFacadeImpl(
            CommandsFactory<StudentCommand<?>> factory,
            BusinessMessagePayloadMapper mapper,
            CommandActionExecutor actionExecutor
    ) {
        this.factory = factory;
        this.actionExecutor = actionExecutor;
        this.toPayload = student -> student instanceof StudentPayload ? student : mapper.toPayload(student);
    }


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
        log.debug("Finding student by ID:{}", id);
        final Optional<Optional<Student>> result;
        result = executeCommand(CommandId.FIND_BY_ID, factory, Input.of(id));
        if (result.isPresent()) {
            final Optional<Student> student = result.get();
            log.debug("Found the student {}", student);
            return student.map(toPayload);
        }
        return Optional.empty();
    }

    /**
     * To get students enrolled to the course
     *
     * @param id system-id of the course
     * @return set of students
     */
    @Override
    public Set<Student> findEnrolledTo(Long id) {
        log.debug("Finding students enrolled to the course with ID:{}", id);
        final Optional<Set<Student>> result;
        result = executeCommand(CommandId.FIND_ENROLLED, factory, Input.of(id));
        if (result.isPresent()) {
            final Set<Student> students = result.get();
            log.debug("Found students enrolled to the course {} ", students);
            return students.stream().map(toPayload).collect(Collectors.toSet());
        }
        return Set.of();
    }

    /**
     * To get students not enrolled to any course
     *
     * @return set of students
     */
    @Override
    public Set<Student> findNotEnrolled() {
        log.debug("Finding students not enrolled to any course");
        final Optional<Set<Student>> result;
        result = executeCommand(CommandId.FIND_NOT_ENROLLED, factory, Input.empty());
        if (result.isPresent()) {
            final Set<Student> students = result.get();
            log.debug("Found students not enrolled to any course {}", students);
            return students.stream().map(toPayload).collect(Collectors.toSet());
        }
        return Set.of();
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
        log.debug("Creating or Updating student {}", instance);
        final var input = Input.of(toPayload.apply(instance));
        final Optional<Optional<Student>> result;
        result = executeCommand(CommandId.CREATE_OR_UPDATE, factory, input);
        if (result.isPresent()) {
            final Optional<Student> student = result.get();
            log.debug("Changed student {}", student);
            return student.map(toPayload);
        }
        return Optional.empty();
    }

    /**
     * To create student instance + it's profile at once
     *
     * @param instance student should be created
     * @return student instance or empty() if not exists
     * @see Student
     * @see Optional
     * @see Optional#empty()
     */
    @Override
    public Optional<Student> create(Student instance) {
        log.debug("Creating student {}", instance);
        final var input = Input.of(toPayload.apply(instance));
        final Optional<Optional<Student>> result;
        result = executeCommand(CommandId.CREATE_NEW, factory, input);
        if (result.isPresent()) {
            final Optional<Student> student = result.get();
            log.debug("Created student {}", student);
            return student.map(toPayload);
        }
        return Optional.empty();
    }

    /**
     * To delete student from the school
     *
     * @param id system-id of the student
     * @return true if success
     * @throws StudentNotFoundException    throws when student it not exists
     * @throws StudentWithCoursesException throws when student is not empty (has enrolled courses)
     */
    @Override
    public boolean delete(Long id) throws StudentNotFoundException, StudentWithCoursesException {
        final String commandId = CommandId.DELETE_ALL;
        final Consumer<Exception> doThisOnError = exception -> {
            switch (exception) {
                case StudentNotFoundException noStudentException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw noStudentException;
                }
                case StudentWithCoursesException studentWithCoursesException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw studentWithCoursesException;
                }
                case null, default -> defaultDoOnError(commandId).accept(exception);
            }
        };

        log.debug("Deleting student with ID:{}", id);
        final Optional<Boolean> result = executeCommand(commandId, factory, Input.of(id), doThisOnError);
        result.ifPresent(executionResult ->
                log.debug("Deleted student with ID:{} successfully:{} .", id, executionResult)
        );
        return result.orElse(false);
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
