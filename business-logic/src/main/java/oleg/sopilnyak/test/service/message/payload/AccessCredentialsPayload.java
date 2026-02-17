package oleg.sopilnyak.test.service.message.payload;

import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;


/**
 * BusinessMessage Payload Type: POJO for AccessCredentials type
 *
 * @see AccessCredentials
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, doNotUseGetters = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AccessCredentialsPayload extends BasePayload<AccessCredentials> implements AccessCredentials {
    // to current valid token
    private String token;
    // valid token for refreshing expired one
    private String refreshToken;
}
