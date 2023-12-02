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
    @Data
    @AllArgsConstructor
    public static class ProfileExtra implements Map.Entry<String, String> {
        private String key;
        private String value;

        @Override
        public String setValue(String value) {
            final String oldValue = this.value;
            this.value = value;
            return oldValue;
        }
    }

}
