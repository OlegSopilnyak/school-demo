package oleg.sopilnyak.test.service.facade.profile;

import oleg.sopilnyak.test.school.common.facade.PersonProfileFacade;

/**
 * Service-Facade: Service for manage person profiles commands
 */
public interface ProfileCommandFacade extends PersonProfileFacade {
    String FIND_BY_ID = "profile.person.findById";
    String FIND_STUDENT_BY_ID = "profile.student.findById";
    String FIND_PRINCIPAL_BY_ID = "profile.principal.findById";
}
