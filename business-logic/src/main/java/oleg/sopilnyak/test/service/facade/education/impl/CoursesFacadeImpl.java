package oleg.sopilnyak.test.service.facade.education.impl;

import oleg.sopilnyak.test.school.common.business.facade.education.CoursesFacade;
import oleg.sopilnyak.test.school.common.business.facade.organization.base.OrganizationFacade;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.exception.education.CourseHasNoRoomException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.exception.education.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.model.education.Course;
import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.facade.ActionFacade;
import oleg.sopilnyak.test.service.facade.education.base.impl.EducationFacadeImpl;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;

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
 * Service: To process command for school's courses facade
 */
@Slf4j
public class CoursesFacadeImpl extends EducationFacadeImpl implements CoursesFacade, ActionFacade {
    public static final String STUDENT_ID_KEY = "student-id";
    public static final String COURSE_ID_KEY = "course-id";
    private final CommandsFactory<? extends RootCommand<?>> factory;
    @Getter
    private final CommandActionExecutor actionExecutor;
    // semantic data to payload converter
    private final UnaryOperator<Course> toPayload;
    //
    // setting up action-methods by action-id
    private final Map<String, Function<Object[], Object>> actions = Map.<String, Function<Object[], Object>>of(
            CoursesFacade.FIND_BY_ID, this::internalFindById,
            CoursesFacade.FIND_REGISTERED, this::internalFindRegisteredFor,
            CoursesFacade.FIND_NOT_REGISTERED, this::internalFindWithoutStudents,
            CoursesFacade.CREATE_OR_UPDATE, this::internalCreateOrUpdate,
            CoursesFacade.DELETE, this::internalDelete,
            CoursesFacade.REGISTER, this::internalRegister,
            CoursesFacade.UN_REGISTER, this::internalUnRegister
    ).entrySet().stream().collect(Collectors.toMap(
                    Map.Entry::getKey, Map.Entry::getValue,
                    (existing, _) -> existing,
                    HashMap::new
            )
    );

    public CoursesFacadeImpl(
            CommandsFactory<CourseCommand<?>> factory,
            BusinessMessagePayloadMapper mapper,
            CommandActionExecutor actionExecutor
    ) {
        this.factory = factory;
        this.actionExecutor = actionExecutor;
        this.toPayload = course -> course instanceof CoursePayload payload ? payload : mapper.toPayload(course);
    }

    /**
     * Facade depends on the action's execution (organization action)
     *
     * @param actionId   the id of the action
     * @param parameters the parameters of the action to execute
     * @return action execution result value
     * @see OrganizationFacade#doActionAndResult(String, Object...)
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
    // to decode login/password from parameters array
    private static Map<String, Long> decodeRegisterArgument(final Object... parameters) {
        if (parameters == null || parameters.length != 2) {
            throw new IllegalArgumentException("Wrong number of parameters for the register/unregister action");
        }
        final Map<String, Long> result = new HashMap<>();
        if (parameters[0] instanceof Long studentId) {
            result.put(STUDENT_ID_KEY, studentId);
        } else if (parameters[0] instanceof Student student) {
            result.put(STUDENT_ID_KEY, student.getId());
        } else {
            throw new InvalidParameterTypeException("String", parameters[0]);
        }
        if (parameters[1] instanceof Long courseId) {
            result.put(COURSE_ID_KEY, courseId);
        } else if (parameters[1] instanceof Course course) {
            result.put(COURSE_ID_KEY, course.getId());
        } else {
            throw new InvalidParameterTypeException("String", parameters[1]);
        }
        return result;
    }

    // to decode course first from parameters array
    private static Course decodeCourseArgument(final Object... parameters) {
        if (parameters == null || parameters.length < 1) {
            throw new IllegalArgumentException("Wrong number of parameters");
        }
        if (parameters[0] instanceof Course value) {
            return value;
        } else {
            throw new InvalidParameterTypeException("Course", parameters[0]);
        }
    }

    // To get the course by ID (for entry-point)
    private Optional<Course> internalFindById(final Object... parameters) {
        return internalFindById(decodeLongArgument(parameters));
    }

    // To get the course by ID (for internal usage)
    private Optional<Course> internalFindById(final Long id) {
        log.debug("Finding course by ID: {}", id);
        final Optional<Optional<Course>> result = executeCommand(FIND_BY_ID, factory, Input.of(id));
        return result.flatMap(course -> {
            log.debug("Found the course {}", course);
            return course.map(toPayload);
        });
    }

    // To get courses registered for the student (for entry-point)
    private Set<Course> internalFindRegisteredFor(final Object... parameters) {
        return internalFindRegisteredFor(decodeLongArgument(parameters));
    }

    // To get courses registered for the student (for internal usage)
    private Set<Course> internalFindRegisteredFor(final Long id) {
        log.debug("Finding courses registered to student with ID:{}", id);
        final Optional<Set<Course>> result = executeCommand(FIND_REGISTERED, factory, Input.of(id));
        return result.map(entities -> {
            log.debug("Found courses registered to student {}", entities);
            return entities.stream().map(toPayload).collect(Collectors.toSet());
        }).orElseGet(Set::of);
    }

    // To get courses without registered students (for entry-point)
    private Set<Course> internalFindWithoutStudents(final Object... parameters) {
        return internalFindWithoutStudents();
    }

    // To get courses without registered students (for internal usage)
    private Set<Course> internalFindWithoutStudents() {
        log.debug("Finding no-students courses");
        final Optional<Set<Course>> result = executeCommand(FIND_NOT_REGISTERED, factory, Input.empty());
        return result.map(entities -> {
            log.debug("Found no-students courses {}", entities);
            return entities.stream().map(toPayload).collect(Collectors.toSet());
        }).orElseGet(Set::of);
    }

    // To create or update course instance (for entry-point)
    private Optional<Course> internalCreateOrUpdate(final Object... parameters) {
        return internalCreateOrUpdate(decodeCourseArgument(parameters));
    }

    // To create or update course instance (for internal usage)
    private Optional<Course> internalCreateOrUpdate(final Course instance) {
        log.debug("Creating or Updating course {}", instance);
        final var input = Input.of(toPayload.apply(instance));
        final Optional<Optional<Course>> result = executeCommand(CREATE_OR_UPDATE, factory, input);
        return result.flatMap(course -> {
            log.debug("Changed course {}", course);
            return course.map(toPayload);
        });
    }

    // To delete course from the school (for entry-point)
    private Void internalDelete(final Object... parameters) {
        internalDelete(decodeLongArgument(parameters));
        return null;
    }

    // To delete course from the school (for internal usage)
    private void internalDelete(final Long id) {
        final String commandId = DELETE;
        // setting up customized errors handler
        final Consumer<Exception> doOnError = exception -> {
            switch (exception) {
                case CourseNotFoundException noCourseException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw noCourseException;
                }
                case CourseWithStudentsException notEmptyCourse -> {
                    logSomethingWentWrong(exception, commandId);
                    throw notEmptyCourse;
                }
                case null, default -> defaultDoOnError(commandId).accept(exception);
            }
        };

        log.debug("Deleting course with ID:{}", id);
        final Optional<Boolean> result = executeCommand(commandId, factory, Input.of(id), doOnError);
        result.ifPresent(executionResult ->
                log.debug("Deleted course with ID:{} successfully:{} .", id, executionResult)
        );
    }

    // To register the student to the school course  (for entry-point)
    private Void internalRegister(final Object... parameters) {
        final Map<String, Long> arguments = decodeRegisterArgument(parameters);
        internalRegister(arguments.get(STUDENT_ID_KEY), arguments.get(COURSE_ID_KEY));
        return null;
    }

    // To register the student to the school course  (for internal usage)
    private void internalRegister(final Long studentId, final Long courseId) {
        final String commandId = REGISTER;
        final Consumer<Exception> doOnError = exception -> {
            switch (exception) {
                case StudentNotFoundException noStudentException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw noStudentException;
                }
                case CourseNotFoundException noCourseException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw noCourseException;
                }
                case CourseHasNoRoomException noRoomException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw noRoomException;
                }
                case StudentCoursesExceedException coursesExceedException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw coursesExceedException;
                }
                case null, default -> defaultDoOnError(commandId).accept(exception);
            }
        };

        log.debug("Registering the student with ID:{} to the course with ID:{}", studentId, courseId);
        final Optional<Boolean> result = executeCommand(commandId, factory, Input.of(studentId, courseId), doOnError);
        result.ifPresent(executionResult ->
                log.debug("Linked course:{} with student:{} successfully:{}.", courseId, studentId, executionResult)
        );
    }

    // To un-register the student from the school course  (for entry-point)
    private Void internalUnRegister(final Object... parameters) {
        final Map<String, Long> arguments = decodeRegisterArgument(parameters);
        internalUnRegister(arguments.get(STUDENT_ID_KEY), arguments.get(COURSE_ID_KEY));
        return null;
    }

    // To un-register the student from the school course  (for internal usage)
    private void internalUnRegister(final Long studentId, final Long courseId) {
        final String commandId = UN_REGISTER;
        final Consumer<Exception> doOnError = exception -> {
            switch (exception) {
                case StudentNotFoundException noStudentException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw noStudentException;
                }
                case CourseNotFoundException noCourseException -> {
                    logSomethingWentWrong(exception, commandId);
                    throw noCourseException;
                }
                case null, default -> defaultDoOnError(commandId).accept(exception);
            }
        };

        log.debug("UnRegistering the student with ID:{} from the course with ID:{}", studentId, courseId);
        final Optional<Boolean> result = executeCommand(commandId, factory, Input.of(studentId, courseId), doOnError);
        result.ifPresent(executionResult ->
                log.debug("Unlinked course:{} from student:{} successfully:{} .", courseId, studentId, executionResult)
        );
    }
}
