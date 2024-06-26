package oleg.sopilnyak.test.school.common.model;

import oleg.sopilnyak.test.school.common.model.base.PersonProfile;

/**
 * Model: Type for person's profile for authority/principal person
 *
 * @see PersonProfile
 */
public interface PrincipalProfile extends PersonProfile {
    /**
     * To get user-name for principal person's login
     *
     * @return value of user-name
     */
    String getLogin();

    /**
     * To check is it the correct password for login
     *
     * @param password password to check
     * @return true if password is correct
     */
    default boolean isPassword(String password){
        return false;
    }
}
