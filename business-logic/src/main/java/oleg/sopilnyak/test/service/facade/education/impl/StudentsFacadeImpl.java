package oleg.sopilnyak.test.service.facade.education.impl;

import oleg.sopilnyak.test.school.common.business.facade.education.StudentsFacade;
import oleg.sopilnyak.test.school.common.business.facade.education.base.EducationFacade;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.facade.ActionFacade;
import oleg.sopilnyak.test.service.facade.education.base.impl.EducationFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Service: To process command for school's student-facade
 */
@Slf4j
public class StudentsFacadeImpl extends EducationFacadeImpl implements StudentsFacade, ActionFacade {
    private final CommandsFactory<StudentCommand<?>> factory;
    @Getter
    private final CommandActionExecutor actionExecutor;
    // semantic data to payload converter
    private final UnaryOperator<Student> toPayload;
    //
    // setting up action-methods by action-id
    private final Map<String, Function<Object[], Object>> actions = Map.<String, Function<Object[], Object>>of(
            StudentsFacade.FIND_BY_ID, this::internalFindById,
            StudentsFacade.FIND_ENROLLED, this::internalEnrolledTo,
            StudentsFacade.FIND_NOT_ENROLLED, this::internalFindNotEnrolled,
            StudentsFacade.CREATE_MACRO, this::internalCreateComposite,
            StudentsFacade.CREATE_OR_UPDATE, this::internalCreateOrUpdate,
            StudentsFacade.DELETE_MACRO, this::internalDeleteComposite
    ).entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey, Map.Entry::getValue,
                    (existing, _) -> existing,
                    HashMap::new
            )
    );

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
     * Facade depends on the action's execution (organization action)
     *
     * @param actionId   the id of the action
     * @param parameters the parameters of the action to execute
     * @return action execution result value
     * @see EducationFacade#doActionAndResult(String, Object...)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T educationAction(final String actionId, final Object... parameters) {
        getLogger().debug("Trying to execute action {} with arguments {}", actionId, parameters);
        return (T) actions.computeIfAbsent(actionId, this::throwsUnknownActionId).apply(parameters);
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

    // private methods
    // to decode student first from parameters array
    private static Student decodeStudentArgument(final Object... parameters) {
        if (parameters == null || parameters.length < 1) {
            throw new IllegalArgumentException("Wrong number of parameters");
        }
        if (parameters[0] instanceof Student value) {
            return value;
        } else {
            throw new InvalidParameterTypeException("Student", parameters[0]);
        }
    }

    // To get the student by ID (for entry-point)
    private Optional<Student> internalFindById(final Object... parameters) {
        return internalFindById(decodeLongArgument(parameters));
    }

    // To get the student by ID (for internal usage)
    private Optional<Student> internalFindById(final Long id) {
        log.debug("Finding student by ID:{}", id);
        final Optional<Optional<Student>> result = executeCommand(FIND_BY_ID, factory, Input.of(id));
        return result.flatMap(student -> {
            log.debug("Found the student {}", student);
            return student.map(toPayload);
        });
    }

    //  To get students enrolled to the course (for entry-point)
    private Set<Student> internalEnrolledTo(final Object... parameters) {
        return internalEnrolledTo(decodeLongArgument(parameters));
    }

    //  To get students enrolled to the course (for internal usage)
    private Set<Student> internalEnrolledTo(final Long id) {
        log.debug("Finding students enrolled to the course with ID:{}", id);
        final Optional<Set<Student>> result = executeCommand(FIND_ENROLLED, factory, Input.of(id));
        return result.map(entities -> {
            log.debug("Found students enrolled to the course {} ", entities);
            return entities.stream().map(toPayload).collect(Collectors.toSet());
        }).orElseGet(Set::of);
    }

    // To get students not enrolled to any course (for entry-point)
    private Set<Student> internalFindNotEnrolled(final Object... parameters) {
        return internalFindNotEnrolled();
    }

    // To get students not enrolled to any course (for internal usage)
    private Set<Student> internalFindNotEnrolled() {
        log.debug("Finding students not enrolled to any course");
        final Optional<Set<Student>> result = executeCommand(FIND_NOT_ENROLLED, factory, Input.empty());
        return result.map(entities -> {
            log.debug("Found students not enrolled to any course {}", entities);
            return entities.stream().map(toPayload).collect(Collectors.toSet());
        }).orElseGet(Set::of);
    }

    // To create or update course instance (for entry-point)
    private Optional<Student> internalCreateOrUpdate(final Object... parameters) {
        return internalCreateOrUpdate(decodeStudentArgument(parameters));
    }

    // To create or update course instance (for internal usage)
    private Optional<Student> internalCreateOrUpdate(final Student instance) {
        log.debug("Creating or Updating student {}", instance);
        final var input = Input.of(toPayload.apply(instance));
        final Optional<Optional<Student>> result = executeCommand(CREATE_OR_UPDATE, factory, input);
        return result.flatMap(student -> {
            log.debug("Changed student {}", student);
            return student.map(toPayload);
        });
    }

    // To create student instance + it's profile at once (for entry-point)
    private Optional<Student> internalCreateComposite(final Object... parameters) {
        return internalCreateComposite(decodeStudentArgument(parameters));
    }

    // To create student instance + it's profile at once (for internal usage)
    private Optional<Student> internalCreateComposite(final Student instance) {
        log.debug("Creating student {}", instance);
        final var input = Input.of(toPayload.apply(instance));
        final Optional<Optional<Student>> result = executeCommand(CREATE_MACRO, factory, input);
        return result.flatMap(student -> {
            log.debug("Created student {}", student);
            return student.map(toPayload);
        });
    }

    // To delete the student with it profile from the school by ID (for entry-point)
    private Void internalDeleteComposite(final Object... parameters) {
        internalDeleteComposite(decodeLongArgument(parameters));
        return null;
    }

    // To delete the student with it profile from the school by ID (for internal usage)
    private void internalDeleteComposite(final Long id) {
        final String commandId = DELETE_MACRO;
        // setting up customized errors handler
        final Consumer<Exception> doOnError = exception -> {
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
        final Optional<Boolean> result = executeCommand(commandId, factory, Input.of(id), doOnError);
        result.ifPresent(executionResult ->
                log.debug("Deleted student with ID:{} successfully:{} .", id, executionResult)
        );
    }
}
