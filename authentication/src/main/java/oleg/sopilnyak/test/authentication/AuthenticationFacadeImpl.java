package oleg.sopilnyak.test.authentication;

import oleg.sopilnyak.test.authentication.model.AccessCredentialsEntity;
import oleg.sopilnyak.test.authentication.model.UserDetailsEntity;
import oleg.sopilnyak.test.authentication.service.JwtService;
import oleg.sopilnyak.test.authentication.service.TokenStorage;
import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;

import jakarta.servlet.Filter;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
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
    // JWT managing service
    private final JwtService jwtService;
    // te storage of active tokens
    private final TokenStorage tokenStorage;
    /**
     * To sign in person to the application
     *
     * @param username person's access username
     * @param password person's access password
     * @return access credentials
     * @throws SchoolAccessDeniedException if access is denied
     * @see AccessCredentials
     */
    @Override
    public AccessCredentials signIn(final String username, final String password) throws SchoolAccessDeniedException {
        final Optional<AccessCredentials> signedIn = tokenStorage.findCredentials(username);
        if (signedIn.isPresent()) {
            log.debug("Found credentials for {}", username);
            return signedIn.get();
        }
        // building credentials for signing in user by username
        log.debug("Signing In person with username '{}'", username);
        // making signing in user-details
        final PrincipalProfile profile = profilePersistenceFacade.findPersonProfileByLogin(username).map(PrincipalProfile.class::cast)
                .orElseThrow(() -> new ProfileNotFoundException("Profile with username: '" + username + "' isn't found"));
        if (!profile.isPassword(password)) {
            log.error("Wrong password for username: '{}'", username);
            throw new SchoolAccessDeniedException("Wrong password for username: " + username);
        }
        log.debug("Password for person with username '{}' is correct", username);
        final UserDetails signingUser = new UserDetailsEntity(username,null, authorities(profile));
        // prepare tokens
        final String validToken = jwtService.generateToken(signingUser);
        final String refreshToken = jwtService.generateRefreshToken(signingUser);
        final AccessCredentials signedInCredentials = AccessCredentialsEntity.builder()
                .id(null).user(signingUser).token(validToken).refreshToken(refreshToken)
                .build();
        // storing built person's access-credentials
        tokenStorage.storeFor(username, signedInCredentials);
        // returns built person's access-credentials
        return signedInCredentials;
    }

    /**
     * To sign out person from the application<BR/>
     * Tokens won't be valid after
     *
     * @param activeToken valid token to sign out the person
     * @see AuthenticationFacade#signIn(String, String)
     * @see AccessCredentials#getToken()
     */
    @Override
    public void signOut(final String activeToken) {
        final String username = jwtService.extractUserName(activeToken);
        final Optional<AccessCredentials> signedIn = tokenStorage.findCredentials(username);
        if (signedIn.isEmpty()) {
            log.debug("No credentials found for username '{}'", username);
            return;
        }
        // signing user out
        log.debug("Signing out user with username '{}' ...", username);
        final AccessCredentials signedInCredentials = signedIn.get();
        final String blackListedToken = signedInCredentials.getToken();
        tokenStorage.toBlackList(blackListedToken);
        log.debug("Added token of '{}' to tokens black list", username);
        tokenStorage.deleteCredentials(username);
        log.debug("Deleted stored credentials of '{}'", username);
    }

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
    @Override
    public AccessCredentials refresh(final String refreshToken) throws SchoolAccessDeniedException {
        final String username = jwtService.extractUserName(refreshToken);
        log.debug("Refreshing token for person with username '{}'", username);
        final AccessCredentials signedIn = tokenStorage.findCredentials(username)
                .orElseThrow(() -> new SchoolAccessDeniedException("Person with username: '" + username + "' isn't signed in"));
        // making new token
        if (signedIn instanceof AccessCredentialsEntity entity) {
            final String validToken = jwtService.generateToken(entity.getUser());
            entity.setToken(validToken);
            tokenStorage.storeFor(username, entity);
            log.debug("Generated and stored token of '{}'", username);
            return signedIn;
        }
        // wrong type of the AccessCredentials ¯\_(ツ)_/¯
        throw new SchoolAccessDeniedException("Person with username: '" + username + "' isn't signed in");
    }

    /**
     * To find credentials of the signed in person
     *
     * @param username username of the person
     * @return active credentials or empty
     * @see Optional
     * @see AccessCredentials
     * @see AuthenticationFacade#signIn(String, String)
     * @see TokenStorage#findCredentials(String)
     */
    @Override
    public Optional<AccessCredentials> findCredentialsFor(String username) {
        return tokenStorage.findCredentials(username);
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

    // private methods
    // to get user's authorities from principal's profile
    private Collection<? extends GrantedAuthority> authorities(final PrincipalProfile profile) {
        return Set.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
