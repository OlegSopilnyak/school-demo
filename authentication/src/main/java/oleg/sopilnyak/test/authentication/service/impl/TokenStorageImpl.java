package oleg.sopilnyak.test.authentication.service.impl;

import oleg.sopilnyak.test.authentication.service.TokenStorage;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Storage: Implementation of the storage of active tokens
 */
public class TokenStorageImpl implements TokenStorage {
    private final Map<String, AccessCredentials> accessCredentials = new ConcurrentHashMap<>();
    private final Set<String> blackList = ConcurrentHashMap.newKeySet();
    /**
     * Storing signed in credentials for further usage
     *
     * @param username            person's access username
     * @param credentials access credentials
     * @see AccessCredentials
     */
    @Override
    public void storeFor(final String username, final AccessCredentials credentials) {
        accessCredentials.put(username, credentials);
    }

    /**
     * Deleting stored person's credentials
     *
     * @param username person's access username
     */
    @Override
    public void deleteCredentials(final String username) {
        accessCredentials.remove(username);
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
        return Optional.ofNullable(accessCredentials.get(username));
    }

    /**
     * To add token to black list for further token's ignoring
     *
     * @param token token to ignore
     */
    @Override
    public void toBlackList(final String token) {
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
        return blackList.contains(token);
    }
}
