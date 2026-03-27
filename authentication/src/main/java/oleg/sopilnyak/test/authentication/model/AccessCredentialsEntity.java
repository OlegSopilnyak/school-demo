package oleg.sopilnyak.test.authentication.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * Model: Entity for school's services access tokens
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessCredentialsEntity implements AccessCredentialsType {
    // the current valid access token
    private String token;
    // valid token for refreshing current one
    private String refreshToken;
    // user-details used for tokens generation
    private UserDetailsEntity user;
}
