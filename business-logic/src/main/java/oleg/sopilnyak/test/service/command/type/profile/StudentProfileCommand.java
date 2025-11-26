package oleg.sopilnyak.test.service.command.type.profile;

import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.service.command.executable.sys.BasicCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;

import java.util.Optional;

import static java.util.Objects.isNull;

/**
 * Type for school-student-profile commands
 */
public interface StudentProfileCommand<T> extends ProfileCommand<T> {
    // command-ids of the command family
    final class CommandId {
        private CommandId() {
        }

        public static final String FIND_BY_ID = "profile.student.findById";
        public static final String CREATE_OR_UPDATE = "profile.student.createOrUpdate";
        public static final String DELETE_BY_ID = "profile.student.deleteById";
    }

    // the name of factory in Spring Beans Factory
    String FACTORY_BEAN_NAME = "studentProfileCommandsFactory";

    // spring-bean component names of the commands family
    final class Component {
        private Component() {
        }

        public static final String FIND_BY_ID = "profileStudentFind";
        public static final String CREATE_OR_UPDATE = "profileStudentUpdate";
        public static final String DELETE_BY_ID = "profileStudentDelete";
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
        return (Class<F>) StudentProfileCommand.class;
    }

    /**
     * To adopt student profile to business-logic data model from persistence data model refreshing entity's relation
     *
     * @param entity entity from persistence layer
     * @param <E>    type of person profile to adopt
     * @return instance from business-logic data model
     * @see StudentProfile#getExtraKeys()
     * @see BusinessMessagePayloadMapper#toPayload(StudentProfile)
     * @see RootCommand#getPayloadMapper()
     */
    @Override
    @SuppressWarnings("unchecked")
    default <E extends PersonProfile> E adoptEntity(final E entity) {
        if (entity instanceof StudentProfile profile) {
            final String [] extraKeys = profile.getExtraKeys();
            final int extraLength = isNull(extraKeys) ? 0 : extraKeys.length;
            getLog().debug("In student profile entity with id={} has {} extra keys", entity.getId(), extraLength);
            final var payload = profile instanceof StudentProfilePayload entityPayload
                    ?
                    entityPayload
                    :
                    getPayloadMapper().toPayload(profile);
            return (E) payload;
        } else {
            throw new UnsupportedOperationException("Entity to adopt is not student-profile");
        }
    }

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
            // To detach StudentProfile optional result entity from persistence layer
            return  optionalEntity.isEmpty() ?
                    (T) Optional.empty()
                    :
                    (T) optionalEntity.map(StudentProfile.class::cast).map(this::detach);
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
