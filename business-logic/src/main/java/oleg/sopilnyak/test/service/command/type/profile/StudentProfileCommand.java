package oleg.sopilnyak.test.service.command.type.profile;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.school.common.model.person.profile.PersonProfile;
import oleg.sopilnyak.test.school.common.model.person.profile.StudentProfile;
import oleg.sopilnyak.test.service.command.executable.core.BasicCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.command.type.core.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;

/**
 * Type for school-student-profile commands
 */
public interface StudentProfileCommand<T> extends ProfileCommand<T> {
    // the name of factory singleton in Spring Beans Factory
    String FACTORY_BEAN_NAME = "studentProfileCommandsFactory";
    //
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
    default <E extends PersonProfile> E adoptEntity(final E entity) {
        return switch (entity) {
            case StudentProfile profile -> adoptProfile(entity, profile);
            default -> throw new UnsupportedOperationException("Entity to adopt is not a student-profile type");
        };
    }

    @SuppressWarnings("unchecked")
    private <E extends PersonProfile> E adoptProfile(final E entity, final StudentProfile profile) {
        final String[] extraKeys = profile.getExtraKeys();
        final int extraLength = isNull(extraKeys) ? 0 : extraKeys.length;
        getLog().debug("In student profile entity with id={} has {} extra keys", entity.getId(), extraLength);
        return (E) (
                profile instanceof StudentProfilePayload profilePayload
                        // not needed transformation
                        ? profilePayload
                        // transforms using payload-mapper
                        : getPayloadMapper().toPayload(profile)
        );
    }

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareNestedContextVisitor#prepareContext(StudentProfileCommand, Input)
     * @see oleg.sopilnyak.test.service.command.executable.core.MacroCommand#createContext(Input)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareNestedContextVisitor visitor, final Input<?> macroInputParameter) {
        return visitor.prepareContext(this, macroInputParameter);
    }
}
