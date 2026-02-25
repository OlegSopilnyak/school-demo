package oleg.sopilnyak.test.authentication;

import oleg.sopilnyak.test.authentication.model.AccessCredentialsEntity;
import oleg.sopilnyak.test.authentication.service.AccessTokensStorage;
import oleg.sopilnyak.test.authentication.service.JwtService;
import oleg.sopilnyak.test.authentication.service.UserService;
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
    // service to find user-details for signed-in users
    private final UserService userService;
    // JWT managing service
    private final JwtService jwtService;
    // the storage of active tokens
    private final AccessTokensStorage tokenStorage;


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
    @Override
    public Optional<AccessCredentials> signIn(final String username, final String password) throws SchoolAccessDeniedException {
        final Optional<AccessCredentials> storedAccessCredentials = tokenStorage.findCredentials(username);
        if (storedAccessCredentials.isPresent()) {
            log.debug("Found credentials for person with username '{}'", username);
            return storedAccessCredentials;
        }
        // building credentials for signing in user by username
        log.debug("Signing In person with username '{}'", username);
        // making signed in user-details for username/password
        return makeAccessCredentialsFor(username, password);
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
    public Optional<AccessCredentials> signOut(final String activeToken) {
        final String username = jwtService.extractUserName(activeToken);
        final Optional<AccessCredentials> signedIn = tokenStorage.findCredentials(username);
        signedIn.ifPresentOrElse(credentials -> {
                    log.debug("Signing out user with username '{}' ...", username);
                    tokenStorage.toBlackList(credentials.getToken());
                    log.debug("Added token of '{}' to tokens black list", username);
                    tokenStorage.deleteCredentials(username);
                    log.debug("Deleted stored credentials of '{}'", username);
                },
                () -> log.debug("No credentials found for username '{}'", username)
        );
        return signedIn;
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
    public Optional<AccessCredentials> refresh(final String refreshToken) throws SchoolAccessDeniedException {
        if (jwtService.isTokenExpired(refreshToken)) {
            log.warn("Refresh token is expired '{}'", refreshToken);
            // removing entity with refresh-token from storage
            tokenStorage.deleteCredentialsWithRefreshToken(refreshToken);
            return Optional.empty();
        }
        final String username = jwtService.extractUserName(refreshToken);
        log.debug("Refreshing token for person with username '{}'", username);
        final AccessCredentials signedIn = tokenStorage.findCredentials(username)
                .orElseThrow(() -> new SchoolAccessDeniedException("Person with username: '" + username + "' isn't signed in"));
        // making new token
        if (signedIn instanceof AccessCredentialsEntity entity) {
            log.debug("Regenerating and store tokens of '{}'", username);
            // regenerating access credentials with fresh tokens
            return makeAccessCredentialsFor(entity.getUser().getUsername(), entity.getUser().getPassword());
        } else {
            // wrong type of the AccessCredentials ¯\_(ツ)_/¯
            throw new SchoolAccessDeniedException("Person with username: '" + username + "' isn't signed in");
        }
    }

    /**
     * To find credentials of the signed in person
     *
     * @param username username of the person
     * @return active credentials or empty
     * @see Optional
     * @see AccessCredentials
     * @see AuthenticationFacade#signIn(String, String)
     * @see AccessTokensStorage#findCredentials(String)
     */
    @Override
    public Optional<AccessCredentials> findCredentialsFor(String username) {
        return tokenStorage.findCredentials(username);
    }

    // private methods
    // making access credentials for username/password
    private Optional<AccessCredentials> makeAccessCredentialsFor(final String username, final String password) {
        return userService.prepareUserDetails(username, password)
                .map(signedInUserDetails -> {
                            log.debug("Preparing user-details for person with username '{}'", username);
                            // prepare access credentials tokens for signed-in user
                            final AccessCredentials signedInCredentials = AccessCredentialsEntity.builder()
                                    .user(signedInUserDetails)
                                    .token(jwtService.generateAccessToken(signedInUserDetails))
                                    .refreshToken(jwtService.generateRefreshToken(signedInUserDetails))
                                    .build();
                            log.debug("Storing built user-details for person with username '{}'", username);
                            // storing built person's access-credentials
                            tokenStorage.storeFor(username, signedInCredentials);
                            return signedInCredentials;
                        }
                );
    }
}
