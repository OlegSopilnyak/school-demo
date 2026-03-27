package oleg.sopilnyak.test.authentication.model;

import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;

/**
 * Model Type: Container for school's services access tokens and user-details
 *
 * @see AccessCredentials
 */
public interface AccessCredentialsType extends AccessCredentials {
    /**
     * To get user-details used for tokens generation
     * @return user-details associated with credentials
     * @see UserDetailsType
     */
    UserDetailsType getUser();
}
