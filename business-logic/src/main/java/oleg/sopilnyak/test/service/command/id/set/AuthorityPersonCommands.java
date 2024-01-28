package oleg.sopilnyak.test.service.command.id.set;

/**
 * Service-Command-Ids: The set of authority-persons-command-ids
 * @see oleg.sopilnyak.test.school.common.model.AuthorityPerson
 */
public interface AuthorityPersonCommands {
    String FIND_ALL = "organization.authority.person.findAll";
    String FIND_BY_ID = "organization.authority.person.findById";
    String CREATE_OR_UPDATE = "organization.authority.person.createOrUpdate";
    String DELETE = "organization.authority.person.delete";
}
