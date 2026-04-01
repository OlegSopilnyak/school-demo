package oleg.sopilnyak.test.authentication;

import oleg.sopilnyak.test.authentication.service.ApplicationAccessFacade;
import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service-Facade: Service implementation for manage security access layer of the school application
 */
@Slf4j
@RequiredArgsConstructor
public class AuthenticationFacadeImpl implements AuthenticationFacade {
    // facade to manage person access credentials
    private final ApplicationAccessFacade accessFacade;


    /**
     * To sign in person to the application
     *
     * @param username person's access username
     * @param password person's access password
     * @return granted person's credentials or empty
     * @throws SchoolAccessDeniedException if access is denied
     * @see ApplicationAccessFacade#findCredentialsFor(String)
     * @see ApplicationAccessFacade#grantCredentialsFor(String, String)
     * @see AccessCredentials
     * @see Optional
     */
    @Override
    public Optional<AccessCredentials> signIn(final String username, final String password) throws SchoolAccessDeniedException {
        log.debug("Signing In person with username {}", username);
        return accessFacade.findCredentialsFor(username).map(credentials -> {
            // credentials for username is found
            log.debug("Found credentials for person with username '{}'", username);
            // returns gotten credentials
            return Optional.of(credentials);
        }).orElseGet(() -> {
            // building credentials for signing in user by username
            log.debug("Granting access credentials for the person with username '{}'", username);
            // making signed in user-details for username/password using access-builder
            return accessFacade.grantCredentialsFor(username, password);
        });
    }

    /**
     * To sign out person from the application<BR/>
     * Tokens won't be valid after
     *
     * @param username valid sign in username of the person
     * @return revoked granted person's credentials or empty
     * @see Optional
     * @see AccessCredentials
     * @see ApplicationAccessFacade#findCredentialsFor(String)
     * @see AuthenticationFacade#signIn(String, String)
     */
    @Override
    public Optional<AccessCredentials> signOut(final String username) {
        log.debug("Signing Out person with username {}", username);
        return accessFacade.findCredentialsFor(username).map(_ -> {
            // credentials for username is found
            log.debug("Revoking credentials for person with username '{}'", username);
            // returns revoked credentials
            return accessFacade.revokeCredentialsFor(username);
        }).orElseGet(() -> {
            // credentials for username is not found
            log.warn("No stored credentials found for username '{}'", username);
            // returns empty
            return Optional.empty();
        });
    }

    /**
     * To refresh active token
     *
     * @param refreshToken   active refresh token of signed in person
     * @param activeUsername username from active SecurityContext
     * @return refreshed credentials
     * @throws SchoolAccessDeniedException person signed out
     * @see AuthenticationFacade#signIn(String, String)
     * @see AuthenticationFacade#signOut(String)
     * @see AccessCredentials#getRefreshToken()
     */
    @Override
    public Optional<AccessCredentials> refresh(final String refreshToken, final String activeUsername)
            throws SchoolAccessDeniedException {
        return accessFacade.refreshCredentialsFor(activeUsername, refreshToken);
    }
}
