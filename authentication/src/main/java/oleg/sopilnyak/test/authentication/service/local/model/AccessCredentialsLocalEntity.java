package oleg.sopilnyak.test.authentication.service.local.model;

import oleg.sopilnyak.test.authentication.model.AccessCredentialsType;
import oleg.sopilnyak.test.authentication.model.UserDetailsType;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * Model: Entity for school's services access tokens
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessCredentialsLocalEntity implements AccessCredentialsType {
    // the current valid access token
    private String token;
    // valid token for refreshing current one
    private String refreshToken;
    // user-details used for tokens generation
    private UserDetailsType user;
}
