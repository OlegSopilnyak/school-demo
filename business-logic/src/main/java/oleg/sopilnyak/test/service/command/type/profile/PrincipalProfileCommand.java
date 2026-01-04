package oleg.sopilnyak.test.service.command.type.profile;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.service.command.executable.core.BasicCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.command.type.core.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.command.type.profile.base.ProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;

/**
 * Type for school-principal-profile commands
 */
public interface PrincipalProfileCommand<T> extends ProfileCommand<T> {
    // command-ids of the command family
    final class CommandId {
        private CommandId() {
        }

        public static final String FIND_BY_ID = "profile.principal.findById";
        public static final String CREATE_OR_UPDATE = "profile.principal.createOrUpdate";
        public static final String DELETE_BY_ID = "profile.principal.deleteById";
    }

    // the name of factory in Spring Beans Factory
    String FACTORY_BEAN_NAME = "principalProfileCommandsFactory";

    // spring-bean component names of the commands family
    final class Component {
        private Component() {
        }

        public static final String FIND_BY_ID = "profilePrincipalFind";
        public static final String CREATE_OR_UPDATE = "profilePrincipalUpdate";
        public static final String DELETE_BY_ID = "profilePrincipalDelete";
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
        return (Class<F>) PrincipalProfileCommand.class;
    }

    /**
     * To adopt principal person profile to business-logic data model from persistence data model refreshing entity's relation
     *
     * @param entity entity from persistence layer
     * @param <E>    type of person profile to adopt
     * @return instance from business-logic data model
     * @see PrincipalProfile#getExtraKeys()
     * @see BusinessMessagePayloadMapper#toPayload(PrincipalProfile)
     * @see RootCommand#getPayloadMapper()
     */
    @Override
    default <E extends PersonProfile> E adoptEntity(final E entity) {
        return switch (entity) {
            case PrincipalProfile profile -> adoptProfile(entity, profile);
            default -> throw new UnsupportedOperationException("Entity to adopt is not a principal-profile type");
        };
    }

    @SuppressWarnings("unchecked")
    private <E extends PersonProfile> E adoptProfile(final E entity, final PrincipalProfile profile) {
        final String[] extraKeys = profile.getExtraKeys();
        final int extraLength = isNull(extraKeys) ? 0 : extraKeys.length;
        getLog().debug("In principal person profile entity with id={} has {} extra keys", entity.getId(), extraLength);
        return (E) (profile instanceof PrincipalProfilePayload entityPayload
                // no needed transformation
                ? entityPayload
                // transforms using payload-mapper
                : getPayloadMapper().toPayload(profile));
    }

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input
     * @return prepared for nested command context
     * @see PrepareNestedContextVisitor#prepareContext(PrincipalProfileCommand, Input)
     * @see oleg.sopilnyak.test.service.command.executable.core.MacroCommand#createContext(Input)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareNestedContextVisitor visitor, final Input<?> macroInputParameter) {
        return visitor.prepareContext(this, macroInputParameter);
    }
}
