package oleg.sopilnyak.test.school.common.model.authentication;

import oleg.sopilnyak.test.school.common.model.BaseType;

/**
 * Model: Type for school's services access tokens
 */
public interface AccessCredentials extends BaseType {
    /**
     * To get access to current valid token
     *
     * @return token's value usually JWT
     */
    String getToken();

    /**
     * To get access to valid token for refreshing expired one
     *
     * @return token's value usually JWT
     * @see AccessCredentials#getToken()
     */
    String getRefreshToken();
}
