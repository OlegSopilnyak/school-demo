package oleg.sopilnyak.test.authentication.service.infinispan.model;

import oleg.sopilnyak.test.authentication.model.AccessCredentialsType;

import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Setter;

/**
 * Model: Protobuf Entity for school's services access tokens
 */
@Setter
@Indexed
@EqualsAndHashCode
public class AccessCredentialsProto implements AccessCredentialsType {
    // the current valid access token
    private String token;
    // valid token for refreshing current one
    private String refreshToken;
    // user-details used for tokens generation
    private UserDetailsProto user;

    @Builder
    @ProtoFactory
    public static AccessCredentialsProto of(String token, String refreshToken, UserDetailsProto user) {
        return new AccessCredentialsProto(token, refreshToken, user);
    }

    @ProtoField(number = 1)
    public String getToken() {
        return token;
    }

    @ProtoField(number = 2)
    public String getRefreshToken() {
        return refreshToken;
    }

    @ProtoField(number = 3)
    public UserDetailsProto getUser() {
        return user;
    }

    // private methods
    private AccessCredentialsProto(String token, String refreshToken, UserDetailsProto user) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.user = user;
    }
}
