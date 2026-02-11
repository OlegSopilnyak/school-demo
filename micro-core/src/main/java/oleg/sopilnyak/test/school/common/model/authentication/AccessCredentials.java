package oleg.sopilnyak.test.school.common.model.authentication;

/**
 * Model: Type for school's services access token
 */
public interface AccessCredentials {
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
