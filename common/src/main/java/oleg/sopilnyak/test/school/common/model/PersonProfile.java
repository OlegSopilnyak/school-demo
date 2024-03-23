package oleg.sopilnyak.test.school.common.model;

import java.util.Optional;

/**
 * Model: Type for person's profile in general
 */
public interface PersonProfile {
    /**
     * To get System-ID of the profile
     *
     * @return the value
     */
    Long getId();

    /**
     * To get the url to person's photo
     *
     * @return valid url or null if there is no photo
     */
    String getPhotoUrl();

    /**
     * To get person's e-mail
     *
     * @return the value
     */
    String getEmail();

    /**
     * To get person's phone number
     *
     * @return the value
     */
    String getPhone();

    /**
     * To get person's location (City, State)
     *
     * @return the value
     */
    String getLocation();

    /**
     * To get keys of profile's extra parameters
     *
     * @return array of keys of extra parameters
     */
    String[] getExtraKeys();

    /**
     * To get the value of extra parameter by key
     *
     * @param key key of extra parameter
     * @return parameter value or empty
     * @see Optional
     * @see Optional#get()
     * @see Optional#empty()
     */
    Optional<String> getExtra(String key);

    default Optional<String> getExtra(ExtraKey key) {
        return key == null ? Optional.empty() : getExtra(key.name());
    }

    /**
     * Enumeration of extra parameter keys
     */
    enum ExtraKey {
        WEB,
        FACEBOOK,
        LINKEDIN,
        YOUTUBE,
        TELEGRAM,
        INSTAGRAM
    }
}
