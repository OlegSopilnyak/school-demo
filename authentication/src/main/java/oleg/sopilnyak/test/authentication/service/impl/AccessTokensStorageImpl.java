package oleg.sopilnyak.test.authentication.service.impl;

import oleg.sopilnyak.test.authentication.model.AccessCredentialsEntity;
import oleg.sopilnyak.test.authentication.service.JwtService;
import oleg.sopilnyak.test.authentication.service.AccessTokensStorage;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Storage: Implementation of the storage of active tokens
 */
@Slf4j
@RequiredArgsConstructor
public class AccessTokensStorageImpl implements AccessTokensStorage {
    private final JwtService jwtService;
    private final Map<String, AccessCredentials> accessCredentials = new ConcurrentHashMap<>();
    private final Set<String> blackList = ConcurrentHashMap.newKeySet();

    /**
     * Storing signed in credentials for further usage
     *
     * @param username    person's access username
     * @param credentials access credentials
     * @see AccessCredentials
     */
    @Override
    public void storeFor(final String username, final AccessCredentials credentials) {
        log.debug("Storing access credentials for {}", username);
        accessCredentials.put(username, credentials);
    }

    /**
     * Deleting stored person's credentials
     *
     * @param username person's access username
     */
    @Override
    public void deleteCredentials(final String username) {
        log.debug("Deleting access credentials for {}", username);
        accessCredentials.remove(username);
    }

    /**
     * Deleting stored person's credentials
     *
     * @param refreshToken person's access refresh-token
     */
    @Override
    public void deleteCredentialsWithRefreshToken(final String refreshToken) {
        accessCredentials.values().stream()
                .filter(credentials -> credentials.getRefreshToken().equals(refreshToken))
                .map(AccessCredentialsEntity.class::cast)
                .map(credentials -> credentials.getUser().getUsername())
                .findFirst().ifPresent(this::deleteCredentials);
    }

    /**
     * To find stored credentials
     *
     * @param username person's access username
     * @return access credentials or empty
     * @see Optional
     * @see AccessCredentials
     */
    @Override
    public Optional<AccessCredentials> findCredentials(final String username) {
        log.debug("Finding access credentials for {}", username);
        return username == null ? Optional.empty() : Optional.ofNullable(accessCredentials.get(username));
    }

    /**
     * To add token to black list for further token's ignoring
     *
     * @param token token to ignore
     */
    @Override
    public void toBlackList(final String token) {
        log.debug("Putting to black list token: '{}'", token);
        blackList.add(token);
    }

    /**
     * To check is the token of signed-out person
     *
     * @param token active token of signed-out person
     * @return true if token is black-listed
     */
    @Override
    public boolean isInBlackList(final String token) {
        log.debug("Checking in black list token: '{}'", token);
        if (jwtService.isTokenExpired(token)) {
            log.debug("Detected expired token: '{}'", token);
            blackList.remove(token);
            return false;
        }
        return blackList.contains(token);
    }
}
