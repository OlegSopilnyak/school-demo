package oleg.sopilnyak.test.service.command.type.profile;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;

import java.util.Optional;

/**
 * Type for school-student-profile commands
 */
public interface StudentProfileCommand<T> extends ProfileCommand<T> {
    /**
     * ID of findById student profile command
     */
    String FIND_BY_ID = "profile.student.findById";
    /**
     * ID of deleteById student profile command
     */
    String DELETE_BY_ID = "profile.student.deleteById";
    /**
     * ID of createOrUpdate student profile command
     */
    String CREATE_OR_UPDATE = "profile.student.createOrUpdate";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "studentProfileCommandsFactory";

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
        } else if (result instanceof StudentProfile entity) {
            return (T) detach(entity);
        } else if (result instanceof Optional<?> optionalEntity) {
            return (T) detach((Optional<StudentProfile>) optionalEntity);
        } else {
            getLog().debug("Won't detach result. Leave it as is:'{}'", result);
            return result;
        }
    }

    /**
     * To detach StudentProfile entity from persistence layer
     *
     * @param entity entity to detach
     * @return detached entity
     * @see #detachedResult(Object)
     */
    private StudentProfile detach(StudentProfile entity) {
        getLog().info("Entity to detach:'{}'", entity);
        return entity instanceof StudentProfilePayload payload ? payload : getPayloadMapper().toPayload(entity);
    }

    /**
     * To detach StudentProfile optional entity from persistence layer
     *
     * @param optionalEntity optional entity to detach
     * @return detached optional entity
     * @see #detachedResult(Object)
     */
    private Optional<StudentProfile> detach(Optional<StudentProfile> optionalEntity) {
        if (isNull(optionalEntity) || optionalEntity.isEmpty()) {
            getLog().info("Result is null or empty");
            return Optional.empty();
        } else {
            getLog().info("Optional entity to detach:'{}'", optionalEntity);
            return optionalEntity.map(this::detach);
        }
    }

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareNestedContextVisitor#prepareContext(StudentProfileCommand, Input)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Input)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareNestedContextVisitor visitor, final Input<?> macroInputParameter) {
        return visitor.prepareContext(this, macroInputParameter);
    }
}
