package oleg.sopilnyak.test.school.common.security;

import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;

import jakarta.servlet.Filter;

/**
 * Service-Facade: Service for manage security access layer of the school application
 */
public interface AuthenticationFacade {
    /**
     * To sign in person to the application
     *
     * @param username person's access username
     * @param password person's access password
     * @return access token
     * @throws SchoolAccessDeniedException if access is denied
     * @see AccessCredentials
     */
    AccessCredentials signIn(String username, String password) throws SchoolAccessDeniedException;

    /**
     * To sign out person from the application<BR/>
     * Token won't valid after
     *
     * @param token valid token to sign out the person
     */
    void signOut(String token);

    /**
     * To get authentication http-filter
     *
     * @return filter's instance
     * @see Filter
     */
    Filter authenticationFilter();
}
