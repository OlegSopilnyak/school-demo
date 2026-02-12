package oleg.sopilnyak.test.school.common.security;

import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;

import jakarta.servlet.Filter;
import java.util.Optional;

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
     * Tokens won't be valid after
     *
     * @param activeToken valid token to sign out the person
     * @see AuthenticationFacade#signIn(String, String)
     * @see AccessCredentials#getToken()
     */
    void signOut(String activeToken);

    /**
     * To refresh active token
     *
     * @param refreshToken active refresh token of signed in person
     * @return refreshed credentials
     * @throws SchoolAccessDeniedException person signed out
     * @see AuthenticationFacade#signIn(String, String)
     * @see AuthenticationFacade#signOut(String)
     * @see AccessCredentials#getRefreshToken()
     */
    AccessCredentials refresh(String refreshToken) throws SchoolAccessDeniedException;

    /**
     * To find credentials of the signed in person
     *
     * @param username username of the person
     * @return active credentials or empty
     * @see Optional
     * @see AccessCredentials
     * @see AuthenticationFacade#signIn(String, String)
     */
    Optional<AccessCredentials> findCredentialsFor(String username);

    /**
     * To get authentication http-filter
     *
     * @return filter's instance
     * @see Filter
     */
    Filter authenticationFilter();
}
