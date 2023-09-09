package oleg.sopilnyak.test.service.facade.organization.entity;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;

/**
 * Service-Facade: To process command for school's authority persons
 * @see AuthorityPerson
 */
public interface AuthorityPersonCommandFacade {
    String FIND_ALL = "organization.authority.person.findAll";
    String FIND_BY_ID = "organization.authority.person.findById";
    String CREATE_OR_UPDATE = "organization.authority.person.createOrUpdate";
    String DELETE = "organization.authority.person.delete";
}
