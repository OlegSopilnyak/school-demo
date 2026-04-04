package oleg.sopilnyak.test.authentication.service.impl;

import oleg.sopilnyak.test.authentication.model.AccessCredentialsType;
import oleg.sopilnyak.test.authentication.model.UserDetailsType;
import oleg.sopilnyak.test.authentication.service.AccessTokensStorage;
import oleg.sopilnyak.test.authentication.service.ApplicationAccessFacade;
import oleg.sopilnyak.test.authentication.service.JwtService;
import oleg.sopilnyak.test.authentication.service.UserService;
import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;

import java.util.Optional;
import org.slf4j.Logger;
import org.springframework.util.ObjectUtils;
import lombok.RequiredArgsConstructor;


/**
 * Service Implementation Adapter: service-facade for access-credentials (abstract storage)
 */
//@Slf4j
@RequiredArgsConstructor
public abstract class ApplicationAccessFacadeAdapter implements ApplicationAccessFacade {
    // service to find user-details for signed-in users
    private final UserService userService;
    // JWT managing service
    protected final JwtService jwtService;
    // the storage of active tokens
    private final AccessTokensStorage tokenStorage;

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
        return tokenStorage.findCredentials(username);
    }

    /**
     * To make access credentials for signing in user's username/password
     *
     * @param username username of signing in user
     * @param password password of signing in user
     * @return valid access tokens or empty
     * @throws SchoolAccessDeniedException if access denied for the user
     * @see UserService#prepareUserDetails(String, String)
     * @see ApplicationAccessFacade#grantCredentialsFor(UserDetailsType)
     * @see Optional
     * @see AccessCredentialsType
     */
    @Override
    public Optional<AccessCredentials> grantCredentialsFor(
            final String username, final String password
    ) throws SchoolAccessDeniedException {
        getLogger().debug("Granting credentials for {}...", username);
        final Optional<AccessCredentials> granted =
                userService.prepareUserDetails(username, password).flatMap(this::grantCredentialsFor);
        granted.ifPresent(credentials -> tokenStorage.storeFor(username, credentials));
        getLogger().debug("{}Granted credentials for {}.", granted.isEmpty() ? "Not " : "", username);
        return granted;
    }

    /**
     * To revoke access credentials for person with username
     *
     * @param username username of signed-in person
     * @return revoked access tokens or empty if person isn't signed in
     */
    @Override
    public Optional<AccessCredentials> revokeCredentialsFor(final String username) {
        return tokenStorage.findCredentials(username).map(credentials -> {
            getLogger().debug("Signing out person with username '{}' ...", username);
            tokenStorage.toBlackList(credentials.getToken());
            getLogger().debug("Added token of '{}' to tokens black list", username);
            tokenStorage.deleteCredentials(username);
            getLogger().debug("Deleted stored credentials of '{}'", username);
            return credentials;
        });
    }

    /**
     * To make access credentials for signed in user-details
     *
     * @param userDetails user-details of signed-in user
     * @return valid access tokens or empty
     * @see Optional
     * @see AccessCredentials
     * @see UserDetailsType
     * @see AuthenticationFacade#refresh(String, String)
     * @see ApplicationAccessFacade#grantCredentialsFor(String, String)
     * @see ApplicationAccessFacadeAdapter#buildFor(UserDetailsType)
     */
    @Override
    public Optional<AccessCredentials> grantCredentialsFor(final UserDetailsType userDetails) {
        return Optional.ofNullable(buildFor(userDetails));
    }

    /**
     * To refresh credentials of signed-in user using refresh-token
     *
     * @param username username of already signed-in user
     * @param token    special token to refresh credentials
     * @return valid access tokens or empty
     * @throws SchoolAccessDeniedException if access is denied for user
     */
    @Override
    public Optional<AccessCredentials> refreshCredentialsFor(final String username, final String token)
            throws SchoolAccessDeniedException {
        return jwtService.isTokenExpired(token) ? refreshTokenIsExpired(token) : refreshedCredentials(username, token);
    }

    /**
     * To build the instance of access credentials for user-details<BR/>
     * Please implement it in the child class
     *
     * @param userDetails user-details of signed-in user
     * @return valid access tokens or null, if it cannot be built
     */
    protected abstract AccessCredentials buildFor(UserDetailsType userDetails);

    /**
     * To get access to facade's logger
     *
     * @return concrete instance of the logger (from child class)
     * @see Logger
     */
    protected abstract Logger getLogger();

    // private methods
    private Optional<AccessCredentials> refreshTokenIsExpired(final String refreshToken) {
        getLogger().warn("Refresh token is expired '{}'", refreshToken);
        // removing entity with refresh-token from storage
        tokenStorage.deleteCredentialsWithRefreshToken(refreshToken);
        return Optional.empty();
    }

    private Optional<AccessCredentials> refreshedCredentials(final String activeUsername, final String refreshToken) {
        // extracting username from refresh-token
        final String username = extractUserName(activeUsername, refreshToken);
        getLogger().debug("Refreshing tokens for person with username '{}'", username);
        final AccessCredentials signedIn = tokenStorage.findCredentials(username)
                .orElseThrow(() -> new SchoolAccessDeniedException("Person with username: '" + username + "' isn't signed in"));
        // making new credentials tokens pair
        getLogger().debug("Rebuilding tokens for person with username '{}' using access {}", username, signedIn);
        return rebuildCredentialsFor(signedIn, username);
    }

    private String extractUserName(final String activeUsername, final String token) {
        final String username = jwtService.extractUserName(token);
        if (!ObjectUtils.nullSafeEquals(username, activeUsername)) {
            getLogger().error("Active user '{}' isn't equals to '{}' from refresh token!", activeUsername, username);
            throw new SchoolAccessDeniedException("Person with username: '" + username + "' isn't signed in");
        }
        return username;
    }

    private Optional<AccessCredentials> rebuildCredentialsFor(final AccessCredentials signedIn, final String username) {
        if (signedIn instanceof AccessCredentialsType entity) {
            getLogger().debug("Regenerating and store tokens of '{}'", username);
            // regenerating access credentials with fresh tokens
            return grantCredentialsFor(entity.getUser());
        } else {
            // wrong type of the AccessCredentials ¯\_(ツ)_/¯
            throw new SchoolAccessDeniedException("Person with username: '" + username + "' isn't signed in");
        }
    }
}
