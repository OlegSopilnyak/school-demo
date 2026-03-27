package oleg.sopilnyak.test.authentication.service.infinispan;

import oleg.sopilnyak.test.authentication.model.AccessCredentialsType;

import org.infinispan.protostream.annotations.Proto;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Model: Entity for school's services access tokens
 */
@Proto
@Data
//@Builder
@NoArgsConstructor
public class AccessCredentialsProto implements AccessCredentialsType {
    // the current valid access token
    public String token;
    // valid token for refreshing current one
    public String refreshToken;
    // user-details used for tokens generation
    public UserDetailsProto user;
}
