package oleg.sopilnyak.test.service.command.type.education;

import static java.util.Objects.isNull;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;

/**
 * Type for school-students command
 *
 * @param <T> the type of command execution (do) result
 * @see RootCommand
 * @see oleg.sopilnyak.test.school.common.model.Student
 */
public interface StudentCommand<T> extends RootCommand<T> {
    String STUDENT_WITH_ID_PREFIX = "Student with ID:";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "studentCommandsFactory";

    /**
     * ID of student's findById command
     */
    String FIND_BY_ID = "student.findById";
    /**
     * ID of student's findEnrolledTo command
     */
    String FIND_ENROLLED = "student.findEnrolledTo";
    /**
     * ID of student's findNotEnrolled command
     */
    String FIND_NOT_ENROLLED = "student.findNotEnrolled";
    /**
     * ID of student's createOrUpdate command
     */
    String CREATE_OR_UPDATE = "student.createOrUpdate";
    /**
     * ID of student's create command
     */
    String CREATE_NEW = "student.create.macro";
    /**
     * ID of student's delete command
     */
    String DELETE = "student.delete";
    /**
     * ID of student's delete macro-command
     */
    String DELETE_ALL = "student.delete.macro";

    /**
     * To detach command result data from persistence layer
     *
     * @param result result data to detach
     * @return detached result data
     * @see oleg.sopilnyak.test.service.command.type.base.RootCommand#afterExecuteDo(Context)
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
            return (T) detach((Optional<Student>) optionalEntity);
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
     * To detach Student optional entity from persistence layer
     *
     * @param optionalEntity optional entity to detach
     * @return detached optional entity
     * @see #detachedResult(Object)
     */
    private Optional<Student> detach(Optional<Student> optionalEntity) {
        if (isNull(optionalEntity) || optionalEntity.isEmpty()) {
            getLog().info("Result is null or empty");
            return Optional.empty();
        } else {
            getLog().info("Optional entity to detach:'{}'", optionalEntity);
            return optionalEntity.map(this::detach);
        }
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
