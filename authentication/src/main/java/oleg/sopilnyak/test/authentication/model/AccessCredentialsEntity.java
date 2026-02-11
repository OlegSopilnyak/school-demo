package oleg.sopilnyak.test.authentication.model;

import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * Model: Entity for school's services access tokens
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessCredentialsEntity implements AccessCredentials {
    // System-ID of the entity
    private Long id;
    // to current valid token
    private String token;
    // valid token for refreshing expired one
    private String refreshToken;
}
