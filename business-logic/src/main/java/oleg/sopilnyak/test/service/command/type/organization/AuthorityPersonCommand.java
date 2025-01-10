package oleg.sopilnyak.test.service.command.type.organization;

import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import oleg.sopilnyak.test.service.command.type.organization.base.OrganizationCommand;

/**
 * Type for school-organization authority persons management command
 *
 * @param <T> the type of command execution (do) result
 * @see OrganizationCommand
 * @see oleg.sopilnyak.test.school.common.model.AuthorityPerson
 */
public interface AuthorityPersonCommand<T> extends OrganizationCommand<T> {
    String PERSON_WITH_ID_PREFIX = "AuthorityPerson with ID:";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "authorityCommandsFactory";
    /**
     * Command-ID: for log in person by login/password
     */
    String LOGIN = "organization.authority.person.login";
    /**
     * Command-ID: for log out person by auth token
     */
    String LOGOUT = "organization.authority.person.logout";
    /**
     * Command-ID: for find all authority persons
     */
    String FIND_ALL = "organization.authority.person.findAll";
    /**
     * Command-ID: for find by ID authority person
     */
    String FIND_BY_ID = "organization.authority.person.findById";
    /**
     * Command-ID: for create or update authority person entity
     */
    String CREATE_OR_UPDATE = "organization.authority.person.createOrUpdate";
    /**
     * Command-ID: for create authority person entity with related profile
     */
    String CREATE_NEW = "organization.authority.person.create.macro";
    /**
     * Command-ID: for delete authority person entity
     */
    String DELETE = "organization.authority.person.delete";
    /**
     * Command-ID: for delete authority person entity with assigned profile
     */
    String DELETE_ALL = "organization.authority.person.delete.macro";

// For commands playing Nested Command Role

    /**
     * To prepare context for nested command using the visitor
     *
     * @param visitor             visitor of prepared contexts
     * @param macroInputParameter Macro-Command call's input parameter
     * @return prepared for nested command context
     * @see PrepareContextVisitor#prepareContext(AuthorityPersonCommand, Input)
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#createContext(Input)
     */
    @Override
    default Context<T> acceptPreparedContext(final PrepareContextVisitor visitor, final Input<?> macroInputParameter) {
        return visitor.prepareContext(this, macroInputParameter);
    }
}
