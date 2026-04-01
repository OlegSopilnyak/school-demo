package oleg.sopilnyak.test.authentication.service;

import oleg.sopilnyak.test.authentication.model.UserDetailsType;
import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;

import java.util.Optional;

/**
 * Service: service-facade for application access management
 */
public interface ApplicationAccessFacade {
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
     * Trying to grant access credentials for person with username/password
     *
     * @param username username of signing in person
     * @param password password of signing in person
     * @return valid access credentials or empty
     * @throws SchoolAccessDeniedException if access is denied for the person
     * @see Optional
     * @see AccessCredentials
     */
    Optional<AccessCredentials> grantCredentialsFor(String username, String password) throws SchoolAccessDeniedException;

    /**
     * To grant access credentials for signed in user-details
     *
     * @param userDetails user-details of signed-in person
     * @return valid access credentials or empty
     * @see Optional
     * @see AccessCredentials
     * @see UserDetailsType
     * @see AuthenticationFacade#refresh(String, String)
     */
    Optional<AccessCredentials> grantCredentialsFor(UserDetailsType userDetails);

    /**
     * To revoke access credentials for person with username
     *
     * @param username username of signed-in person
     * @return revoked access credentials or empty if person isn't signed in
     * @see Optional
     * @see AccessCredentials
     */
    Optional<AccessCredentials> revokeCredentialsFor(String username);

    /**
     * To refresh credentials of signed-in person using refresh-token
     *
     * @param activeUsername username of signed-in person
     * @param refreshToken special token to refresh credentials
     * @return valid access credentials or empty
     * @throws SchoolAccessDeniedException if access is denied for the person
     * @see Optional
     * @see AccessCredentials
     */
    Optional<AccessCredentials> refreshCredentialsFor(String activeUsername, String refreshToken) throws SchoolAccessDeniedException;
}
