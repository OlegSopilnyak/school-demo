package oleg.sopilnyak.test.school.common.security;

import oleg.sopilnyak.test.school.common.exception.accsess.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.model.BearerToken;

import jakarta.servlet.Filter;

/**
 * Service-Facade: Service for manage security access layer of the school application
 */
public interface AuthenticationFacade {
    /**
     * To log in person to the application
     *
     * @param username person's access username
     * @param password person's access password
     * @return access token
     * @throws SchoolAccessDeniedException if access is denied
     * @see BearerToken
     */
    BearerToken login(String username, String password) throws SchoolAccessDeniedException;

    /**
     * To log out person from the application<BR/>
     * Token won't valid after
     *
     * @param token valid token to log out the person
     * @see BearerToken#isValid()
     */
    void logout(BearerToken token);

    /**
     * To get authentication http-filter
     *
     * @return filter's instance
     * @see Filter
     */
    Filter authenticationFilter();
}
