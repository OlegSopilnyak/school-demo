package oleg.sopilnyak.test.school.common.security;

import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;

import java.util.Optional;

/**
 * Service-Facade: Service for manage security access layer of the school application
 */
public interface AuthenticationFacade {
    String AUTH_PATH_PREFIX = "/authentication";

    /**
     * To sign in person to the application
     *
     * @param username person's access username
     * @param password person's access password
     * @return signed in credentials or empty
     * @throws SchoolAccessDeniedException if access is denied
     * @see AccessCredentials
     * @see Optional
     */
    Optional<AccessCredentials> signIn(String username, String password) throws SchoolAccessDeniedException;

    /**
     * To sign out person from the application<BR/>
     * Tokens won't be valid after
     *
     * @param activeToken valid token to sign out the person
     * @see AuthenticationFacade#signIn(String, String)
     * @see AccessCredentials#getToken()
     * @return signed out credentials or empty
     * @see Optional
     */
    Optional<AccessCredentials> signOut(String activeToken);

    /**
     * To refresh active token
     *
     * @param refreshToken active refresh token of signed in person
     * @return refreshed credentials or empty
     * @throws SchoolAccessDeniedException person signed out
     * @see Optional
     */
    Optional<AccessCredentials> refresh(String refreshToken) throws SchoolAccessDeniedException;

    /**
     * To find credentials of the signed in person
     *
     * @param username username of the person
     * @return active credentials or empty
     * @see Optional
     * @see AccessCredentials
     */
    Optional<AccessCredentials> findCredentialsFor(String username);
}
