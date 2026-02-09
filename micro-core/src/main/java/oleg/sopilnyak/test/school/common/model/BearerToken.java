package oleg.sopilnyak.test.school.common.model;

/**
 * Model: Type for school's services access token
 */
public interface BearerToken {
    String BEARER_TOKEN = "Bearer ";

    /**
     * To get access token
     *
     * @return token's value
     */
    String getToken();

    /**
     * To check is token valid
     *
     * @return true if token is valid
     */
    boolean isValid();
}
