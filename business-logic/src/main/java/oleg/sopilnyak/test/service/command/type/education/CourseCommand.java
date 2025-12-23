package oleg.sopilnyak.test.service.command.type.education;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.executable.sys.BasicCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Type for school-courses command
 *
 * @param <T> the type of command execution (do) result
 * @see RootCommand
 * @see Course
 */
public interface CourseCommand<T> extends RootCommand<T> {
    // template of error message
    String COURSE_WITH_ID_PREFIX = "Course with ID:";

    // command-ids of the command family
    final class CommandId {
        private CommandId() {
        }

        public static final String FIND_BY_ID = "course.findById";
        public static final String FIND_REGISTERED = "course.findRegisteredFor";
        public static final String FIND_NOT_REGISTERED = "course.findWithoutStudents";
        public static final String CREATE_OR_UPDATE = "course.createOrUpdate";
        public static final String DELETE = "course.delete";
        public static final String REGISTER = "course.register";
        public static final String UN_REGISTER = "course.unRegister";
    }

    // the name of factory in Spring Beans Factory
    String FACTORY_BEAN_NAME = "courseCommandsFactory";

    // spring-bean component names of the commands family
    final class Component {
        private Component() {
        }

        public static final String FIND_BY_ID = "courseFind";
        public static final String FIND_REGISTERED = "courseFindWithStudents";
        public static final String FIND_NOT_REGISTERED = "courseFindNoStudents";
        public static final String CREATE_OR_UPDATE = "courseUpdate";
        public static final String DELETE = "courseDelete";
        public static final String REGISTER = "courseRegisterStudent";
        public static final String UN_REGISTER = "courseUnRegisterStudent";
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
        return (Class<F>) CourseCommand.class;
    }

    /**
     * To adopt course entity to business-logic data model from persistence data model refreshing entity's relation
     *
     * @param entity entity from persistence layer
     * @return instance from business-logic data model
     * @see Course#getStudents()
     * @see oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper#toPayload(Course)
     * @see RootCommand#getPayloadMapper()
     */
    default CoursePayload adoptEntity(final Course entity) {
        getLog().debug("In course entity with id={} registered {} student(s)", entity.getId(), entity.getStudents().size());
        return entity instanceof CoursePayload entityPayload ? entityPayload : getPayloadMapper().toPayload(entity);
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
        } else if (result instanceof Course entity) {
            return (T) detach(entity);
        } else if (result instanceof Optional<?> optionalEntity) {
            // To detach Course optional result entity from persistence layer
            return  optionalEntity.isEmpty() ?
                    (T) Optional.empty()
                    :
                    (T) optionalEntity.map(Course.class::cast).map(this::detach);
        } else if (result instanceof Set<?> entitiesSet) {
            return (T) detach((Set<Course>) entitiesSet);
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
        getLog().debug("Entity to detach:'{}'", entity);
        return entity instanceof CoursePayload payload ? payload : getPayloadMapper().toPayload(entity);
    }

    /**
     * To detach Course entities set from persistence layer
     *
     * @param entitiesSet entities set to detach
     * @return detached entities set
     * @see #detachedResult(Object)
     */
    private Set<Course> detach(Set<Course> entitiesSet) {
        getLog().debug("Entities set to detach:'{}'", entitiesSet);
        return entitiesSet.stream().map(this::detach).collect(Collectors.toSet());
    }

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareNestedContextVisitor#prepareContext(CourseCommand, Input)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Input)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareNestedContextVisitor visitor, final Input<?> macroInputParameter) {
        return visitor.prepareContext(this, macroInputParameter);
    }

}
