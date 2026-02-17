package oleg.sopilnyak.test.endpoint.dto;

import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;


/**
 * DataTransportObject: POJO for AccessCredentials type
 *
 * @see oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials
 */
@Data
@Builder
@EqualsAndHashCode
@ToString(doNotUseGetters = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessCredentialsDto implements AccessCredentials {
    // current valid token
    private String token;
    // valid token for refreshing expired one
    private String refreshToken;
    // System-ID of the model's item
    @JsonIgnore
    @Builder.Default
    private Long id = 0L;
}
