package oleg.sopilnyak.test.service.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import lombok.*;
import oleg.sopilnyak.test.school.common.model.base.PersonProfile;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * BusinessMessage Payload Type: POJO for PersonProfile type (the parent of any type of profiles)
 *
 * @see PersonProfile
 * @see Extra
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BasePersonProfile implements PersonProfile {
    private Long id;
    private String photoUrl;
    private String email;
    private String phone;
    private String location;
    private Extra[] extras;

    /**
     * To get keys of profile's extra parameters
     *
     * @return array of keys of extra parameters
     */
    @JsonIgnore
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
                Stream.of(extras).filter(extra -> extra.getKey().equals(key)).map(Extra::getValue).findFirst();
    }

    /**
     * Entry for extra parameter
     *
     * @see Map.Entry
     */
    @Data
    @ToString
    @Builder
    @AllArgsConstructor
    @JsonDeserialize(using = Extra.Deserializer.class)
    public static class Extra implements Map.Entry<String, String> {
        private String key;
        private String value;

        @Override
        public String setValue(String value) {
            var oldValue = this.value;
            this.value = value;
            return oldValue;
        }

        private static class Deserializer extends StdDeserializer<Extra> {
            public Deserializer() {
                super(Extra.class);
            }

            @Override
            public Extra deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
                final JsonNode node = p.getCodec().readTree(p);
                final String key = node.fieldNames().next();
                final String value = node.findValue(key).textValue();
                return new Extra(key, value);
            }
        }
    }

}
