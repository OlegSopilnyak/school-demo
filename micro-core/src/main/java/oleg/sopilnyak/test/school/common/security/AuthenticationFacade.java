package oleg.sopilnyak.test.school.common.security;

import oleg.sopilnyak.test.school.common.exception.accsess.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.model.BearerToken;
import oleg.sopilnyak.test.school.common.model.SchoolAccess;

/**
 * Service-Facade: Service for manage security access layer of the school application
 */
public interface AuthenticationFacade {
    /**
     * To log in person to the application
     *
     * @param schoolAccess  access properties
     * @return access token
     * @throws SchoolAccessDeniedException if access is denied
     */
    BearerToken login(SchoolAccess schoolAccess) throws SchoolAccessDeniedException;

    /**
     * To log out person from the application<BR/>
     * Token won't valid after
     *
     * @param token valid token to log out the person
     * @see BearerToken#isValid()
     */
    void logout(BearerToken token);
}
