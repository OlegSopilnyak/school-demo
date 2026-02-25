package oleg.sopilnyak.test.authentication.service;

import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;

import java.util.Optional;

/**
 * Storage: the storage of active tokens
 */
public interface AccessTokensStorage {
    /**
     * Storing signed in credentials for further usage
     *
     * @param username            person's access username
     * @param signedInCredentials access credentials
     * @see AccessCredentials
     */
    void storeFor(String username, AccessCredentials signedInCredentials);

    /**
     * Deleting stored person's credentials
     *
     * @param username person's access username
     */
    void deleteCredentials(String username);

    /**
     * Deleting stored person's credentials
     *
     * @param refreshToken person's access refresh-token
     */
    void deleteCredentialsWithRefreshToken(String refreshToken);

    /**
     * To find stored credentials
     *
     * @param username person's access username
     * @return access credentials or empty
     * @see Optional
     * @see AccessCredentials
     */
    Optional<AccessCredentials> findCredentials(String username);

    /**
     * To add token to black list for further token's ignoring
     *
     * @param blackListedToken token to ignore
     */
    void toBlackList(String blackListedToken);

    /**
     * To check is the token of signed-out person
     *
     * @param token active token of signed-out person
     * @return true if token is black-listed
     */
    boolean isInBlackList(String token);
}
