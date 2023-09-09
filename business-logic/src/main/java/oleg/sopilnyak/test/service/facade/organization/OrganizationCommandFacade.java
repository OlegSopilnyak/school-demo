package oleg.sopilnyak.test.service.facade.organization;

import oleg.sopilnyak.test.school.common.facade.OrganizationFacade;
import oleg.sopilnyak.test.service.facade.organization.entity.AuthorityPersonCommandFacade;
import oleg.sopilnyak.test.service.facade.organization.entity.FacultyCommandFacade;
import oleg.sopilnyak.test.service.facade.organization.entity.StudentsGroupCommandFacade;

/**
 * Service-Facade: To process command for school's organization structure
 */
public interface OrganizationCommandFacade extends
        OrganizationFacade,
        AuthorityPersonCommandFacade,
        FacultyCommandFacade,
        StudentsGroupCommandFacade
{
}
