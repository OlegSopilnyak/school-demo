package oleg.sopilnyak.test.service.command.type.organization;

import oleg.sopilnyak.test.service.command.type.organization.base.OrganizationCommand;

/**
 * Type for school-organization authority persons management command
 *
 * @see OrganizationCommand
 * @see oleg.sopilnyak.test.school.common.model.AuthorityPerson
 */
public interface AuthorityPersonCommand extends OrganizationCommand {
    String PERSON_WITH_ID_PREFIX = "AuthorityPerson with ID:";
    /**
     * The name of commands-factory SpringBean
     */
    String FACTORY_BEAN_NAME = "authorityCommandsFactory";
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
     * Command-ID: for delete authority person entity
     */
    String DELETE = "organization.authority.person.delete";
}
