package oleg.sopilnyak.test.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import oleg.sopilnyak.test.school.common.model.PersonProfile;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.util.ObjectUtils.isEmpty;


/**
 * DataTransportObject: POJO for PersonProfile type (the parent of any type of profiles)
 *
 * @see PersonProfile
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class PersonProfileDto implements PersonProfile {
    private Long id;
    private String photoUrl;
    private String email;
    private String phone;
    private String location;
    private ProfileExtra[] extras;

    /**
     * To get keys of profile's extra parameters
     *
     * @return array of keys of extra parameters
     */
    @Override
    public String[] getExtraKeys() {
        return isEmpty(extras) ? new String[0] :
                Stream.of(extras).map(extra -> extra.key).toArray(String[]::new);
    }

    /**
     * To get the value of extra parameter by key
     *
     * @param key key of extra parameter
     * @return parameter value or empty
     * @see Optional
     * @see Optional#get()
     * @see Optional#empty()
     */
    @Override
    public Optional<String> getExtra(String key) {
        return isEmpty(extras) ? Optional.empty() :
                Stream.of(extras).filter(extra -> extra.getKey().equals(key)).map(ProfileExtra::getValue).findFirst();
    }

    /**
     * Entry for extra parameter
     */
//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProfileExtra implements Map.Entry<String, String> {
        private String key;
        private String value;

        public ProfileExtra() {
            key = "";
            value = "";
        }

        public ProfileExtra(String key, String value) {
            this.key = key;
            this.value = value;
        }

        /**
         * Returns the key corresponding to this entry.
         *
         * @return the key corresponding to this entry
         * @throws IllegalStateException implementations may, but are not
         *                               required to, throw this exception if the entry has been
         *                               removed from the backing map.
         */
        @Override
        public String getKey() {
            return key;
        }

        /**
         * Returns the value corresponding to this entry.  If the mapping
         * has been removed from the backing map (by the iterator's
         * {@code remove} operation), the results of this call are undefined.
         *
         * @return the value corresponding to this entry
         * @throws IllegalStateException implementations may, but are not
         *                               required to, throw this exception if the entry has been
         *                               removed from the backing map.
         */
        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String setValue(String value) {
            final String oldValue = this.value;
            this.value = value;
            return oldValue;
        }
    }

}
