package oleg.sopilnyak.test.service.command.type.education;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.sys.BasicCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Type for school-students command
 *
 * @param <T> the type of command execution (do) result
 * @see RootCommand
 * @see Student
 */
public interface StudentCommand<T> extends RootCommand<T> {
    // template of error message
    String STUDENT_WITH_ID_PREFIX = "Student with ID:";

    // command-ids of the command family
    final class CommandId {
        private CommandId() {
        }

        public static final String FIND_BY_ID = "student.findById";
        public static final String FIND_ENROLLED = "student.findEnrolledTo";
        public static final String FIND_NOT_ENROLLED = "student.findNotEnrolled";
        public static final String CREATE_OR_UPDATE = "student.createOrUpdate";
        public static final String CREATE_NEW = "student.create.macro";
        public static final String DELETE = "student.delete";
        public static final String DELETE_ALL = "student.delete.macro";
    }

    // the name of factory in Spring Beans Factory
    String FACTORY_BEAN_NAME = "studentCommandsFactory";

    // spring-bean component names of the commands family
    final class Component {
        private Component() {
        }

        public static final String FIND_BY_ID = "studentFind";
        public static final String FIND_ENROLLED = "studentFindEnrolled";
        public static final String FIND_NOT_ENROLLED = "studentFindNotEnrolled";
        public static final String CREATE_OR_UPDATE = "studentUpdate";
        public static final String CREATE_NEW = "studentMacroCreate";
        public static final String DELETE = "studentDelete";
        public static final String DELETE_ALL = "studentMacroDelete";
    }

    /**
     * The class of commands family, the command is belonged to
     *
     * @return command family class value
     * @see BasicCommand#self()
     */
    @Override
    @SuppressWarnings("unchecked")
    default <F extends RootCommand> Class<F> commandFamily() {
        return (Class<F>) StudentCommand.class;
    }

    /**
     * To adopt student entity to business-logic data model from persistence data model refreshing entity's relation
     *
     * @param entity entity from persistence layer
     * @return instance from business-logic data model
     * @see Student#getCourses()
     * @see oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper#toPayload(Student)
     * @see RootCommand#getPayloadMapper()
     */
    default StudentPayload adoptEntity(final Student entity) {
        getLog().debug("In student entity with id={} registered {} course(s)", entity.getId(), entity.getCourses().size());
        return entity instanceof StudentPayload entityPayload ? entityPayload : getPayloadMapper().toPayload(entity);
    }

    /**
     * To detach command result data from persistence layer
     *
     * @param result result data to detach
     * @return detached result data
     * @see oleg.sopilnyak.test.service.command.type.base.RootCommand#afterExecute(Context)
     */
    @Override
    @SuppressWarnings("unchecked")
    default T detachedResult(T result) {
        if (isNull(result)) {
            getLog().debug("Result is null");
            return null;
        } else if (result instanceof Student entity) {
            return (T) detach(entity);
        } else if (result instanceof Optional<?> optionalEntity) {
            // To detach Student optional result entity from persistence layer
            return  optionalEntity.isEmpty() ?
                    (T) Optional.empty()
                    :
                    (T) optionalEntity.map(Student.class::cast).map(this::detach);
        } else if (result instanceof Set entitiesSet) {
            return (T) detach(entitiesSet);
        } else {
            getLog().debug("Won't detach result. Leave it as is:'{}'", result);
            return result;
        }
    }

    /**
     * To detach Student entity from persistence layer
     *
     * @param entity entity to detach
     * @return detached entity
     * @see #detachedResult(Object)
     */
    private Student detach(Student entity) {
        getLog().info("Entity to detach:'{}'", entity);
        return entity instanceof StudentPayload payload ? payload : getPayloadMapper().toPayload(entity);
    }

    /**
     * To detach Student entities set from persistence layer
     *
     * @param entitiesSet entities set to detach
     * @return detached entities set
     * @see #detachedResult(Object)
     */
    private Set<Student> detach(Set<Student> entitiesSet) {
        getLog().info("Entities set to detach:'{}'", entitiesSet);
        return entitiesSet.stream().map(this::detach).collect(Collectors.toSet());
    }

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor        visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareNestedContextVisitor#prepareContext(StudentCommand, Input)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Input)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareNestedContextVisitor visitor, final Input<?> macroInputParameter) {
        return visitor.prepareContext(this, macroInputParameter);
    }
}
