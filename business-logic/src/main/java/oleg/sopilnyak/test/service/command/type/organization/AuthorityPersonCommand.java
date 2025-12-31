package oleg.sopilnyak.test.service.command.type.organization;

import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.service.command.executable.sys.BasicCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.PrepareNestedContextVisitor;
import oleg.sopilnyak.test.service.command.type.organization.base.OrganizationCommand;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;

/**
 * Type for school-organization authority persons management command
 *
 * @param <T> the type of command execution (do) result
 * @see OrganizationCommand
 * @see oleg.sopilnyak.test.school.common.model.AuthorityPerson
 */
public interface AuthorityPersonCommand<T> extends OrganizationCommand<T> {
    // template of error message
    String PERSON_WITH_ID_PREFIX = "AuthorityPerson with ID:";

    // command-ids of the command family
    final class CommandId {
        private CommandId() {
        }

        public static final String LOGIN = "organization.authority.person.login";
        public static final String LOGOUT = "organization.authority.person.logout";
        public static final String FIND_ALL = "organization.authority.person.findAll";
        public static final String FIND_BY_ID = "organization.authority.person.findById";
        public static final String CREATE_OR_UPDATE = "organization.authority.person.createOrUpdate";
        public static final String CREATE_NEW = "organization.authority.person.create.macro";
        public static final String DELETE = "organization.authority.person.delete";
        public static final String DELETE_ALL = "organization.authority.person.delete.macro";
    }

    // the name of factory in Spring Beans Factory
    String FACTORY_BEAN_NAME = "authorityCommandsFactory";

    // spring-bean component names of the commands family
    final class Component {
        private Component() {
        }

        public static final String LOGIN = "authorityPersonLogin";
        public static final String LOGOUT = "authorityPersonLogout";
        public static final String FIND_ALL = "authorityPersonFindAll";
        public static final String FIND_BY_ID = "authorityPersonFind";
        public static final String CREATE_OR_UPDATE = "authorityPersonUpdate";
        public static final String CREATE_NEW = "authorityPersonMacroCreate";
        public static final String DELETE = "authorityPersonDelete";
        public static final String DELETE_ALL = "authorityPersonMacroDelete";
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
        return (Class<F>) AuthorityPersonCommand.class;
    }

    /**
     * To adopt authority person entity to business-logic data model from persistence data model refreshing entity's relation
     *
     * @param entity entity from persistence layer
     * @return instance from business-logic data model
     * @see AuthorityPerson#getFaculties()
     * @see oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper#toPayload(AuthorityPerson)
     * @see RootCommand#getPayloadMapper()
     */
    default AuthorityPersonPayload adoptEntity(final AuthorityPerson entity) {
        final int facultiesCount = entity.getFaculties() == null ? 0 : entity.getFaculties().size();
        getLog().debug("In authority person entity with id={} manages {} faculties", entity.getId(), facultiesCount);
        return entity instanceof AuthorityPersonPayload entityPayload ? entityPayload : getPayloadMapper().toPayload(entity);
    }

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input parameter
     * @return prepared for nested command context
     * @see PrepareNestedContextVisitor#prepareContext(AuthorityPersonCommand, Input)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Input)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareNestedContextVisitor visitor, final Input<?> macroInputParameter) {
        return visitor.prepareContext(this, macroInputParameter);
    }
}
