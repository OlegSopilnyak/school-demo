package oleg.sopilnyak.test.authentication;

import oleg.sopilnyak.test.authentication.model.AccessCredentialsEntity;
import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;

import jakarta.servlet.Filter;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service-Facade: Service implementation for manage security access layer of the school application
 */
@Slf4j
@RequiredArgsConstructor
public class AuthenticationFacadeImpl implements AuthenticationFacade {
    // principal person profile persistence facade
    private final ProfilePersistenceFacade profilePersistenceFacade;
    /**
     * To sign in person to the application
     *
     * @param username person's access username
     * @param password person's access password
     * @return access token
     * @throws SchoolAccessDeniedException if access is denied
     * @see AccessCredentials
     */
    @Override
    public AccessCredentials signIn(final String username, final String password) throws SchoolAccessDeniedException {
        log.debug("Signing In person with username '{}'", username);
        final PrincipalProfile profile = profilePersistenceFacade.findPersonProfileByLogin(username).map(PrincipalProfile.class::cast)
                .orElseThrow(() -> new ProfileNotFoundException("Profile with username: '" + username + "' isn't found"));
        if (!profile.isPassword(password)) {
            log.error("Wrong password for username: '{}'", username);
            throw new SchoolAccessDeniedException("Wrong password for username: " + username);
        }
        log.debug("Password for person with username '{}' is correct", username);
        return AccessCredentialsEntity.builder().id(null).token("token").refreshToken("refreshToken").build();
    }

    /**
     * To sign out person from the application<BR/>
     * Tokens won't be valid after
     *
     * @param credentials valid tokens to sign out the person
     * @see AuthenticationFacade#signIn(String, String)
     * @see AccessCredentials
     */
    @Override
    public void signOut(AccessCredentials credentials) {

    }

    /**
     * To refresh active token
     *
     * @param credentials active tokens of signed in person
     * @return refreshed credentials
     * @throws SchoolAccessDeniedException person signed out
     * @see AuthenticationFacade#signIn(String, String)
     * @see AuthenticationFacade#signOut(AccessCredentials)
     * @see AccessCredentials#getRefreshToken()
     */
    @Override
    public AccessCredentials refresh(AccessCredentials credentials) throws SchoolAccessDeniedException {
        return null;
    }

    /**
     * To find credentials of the signed in person
     *
     * @param username username of the person
     * @return active credentials or empty
     * @see Optional
     * @see AccessCredentials
     * @see AuthenticationFacade#signIn(String, String)
     */
    @Override
    public Optional<AccessCredentials> findCredentialsFor(String username) {
        return Optional.empty();
    }

    /**
     * To get authentication http-filter
     *
     * @return filter's instance
     * @see Filter
     */
    @Override
    public Filter authenticationFilter() {
        return null;
    }
}
