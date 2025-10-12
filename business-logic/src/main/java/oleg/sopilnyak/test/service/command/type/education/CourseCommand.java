package oleg.sopilnyak.test.service.command.type.education;

import static java.util.Objects.isNull;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;

/**
 * Type for school-courses command

 * @param <T> the type of command execution (do) result
 * @see RootCommand
 * @see Course
 */
public interface CourseCommand<T> extends RootCommand<T> {
    String COURSE_WITH_ID_PREFIX = "Course with ID:";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "courseCommandsFactory";

    String FIND_BY_ID = "course.findById";
    String FIND_REGISTERED = "course.findRegisteredFor";
    String FIND_NOT_REGISTERED = "course.findWithoutStudents";
    String CREATE_OR_UPDATE = "course.createOrUpdate";
    String DELETE = "course.delete";
    String REGISTER = "course.register";
    String UN_REGISTER = "course.unRegister";

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
        } else if (result instanceof Course entity) {
            return (T) detach(entity);
        } else if (result instanceof Optional<?> optionalEntity) {
            return (T) detach((Optional<Course>) optionalEntity);
        } else if (result instanceof Set entitiesSet) {
            return (T) detach(entitiesSet);
        } else {
            getLog().debug("Won't detach result. Leave it as is:'{}'", result);
            return result;
        }
    }

    /**
     * To detach Course entity from persistence layer
     *
     * @param entity entity to detach
     * @return detached entity
     * @see #detachedResult(Object)
     */
    private Course detach(Course entity) {
        getLog().info("Entity to detach:'{}'", entity);
        return entity instanceof CoursePayload payload ? payload : getPayloadMapper().toPayload(entity);
    }

    /**
     * To detach Course optional entity from persistence layer
     *
     * @param optionalEntity optional entity to detach
     * @return detached optional entity
     * @see #detachedResult(Object)
     */
    private Optional<Course> detach(Optional<Course> optionalEntity) {
        if (isNull(optionalEntity) || optionalEntity.isEmpty()) {
            getLog().info("Result is null or empty");
            return Optional.empty();
        } else {
            getLog().info("Optional entity to detach:'{}'", optionalEntity);
            return optionalEntity.map(this::detach);
        }
    }

    /**
     * To detach Course entities set from persistence layer
     *
     * @param entitiesSet entities set to detach
     * @return detached entities set
     * @see #detachedResult(Object)
     */
    private Set<Course> detach(Set<Course> entitiesSet) {
        getLog().info("Entities set to detach:'{}'", entitiesSet);
        return entitiesSet.stream().map(this::detach).collect(Collectors.toSet());
    }

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor visitor of prepared contexts
     * @param macroInputParameter   Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareNestedContextVisitor#prepareContext(CourseCommand, Input)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Input)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareNestedContextVisitor visitor, final Input<?> macroInputParameter) {
        return visitor.prepareContext(this, macroInputParameter);
    }

}
