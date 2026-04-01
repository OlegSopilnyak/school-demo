package oleg.sopilnyak.test.authentication.service.infinispan.model;

import oleg.sopilnyak.test.authentication.model.AccessCredentialsType;
import oleg.sopilnyak.test.authentication.model.UserDetailsType;

import org.infinispan.protostream.annotations.ProtoField;

/**
 * Model: Protobuf Entity for school's services access tokens
 */
public class AccessCredentialsProto implements AccessCredentialsType {
    // the current valid access token
    private String token;
    // valid token for refreshing current one
    private String refreshToken;
    // user-details used for tokens generation
    private UserDetailsType user;

    @ProtoField(number = 1)
    public String getToken() {
        return token;
    }

    @ProtoField(number = 2)
    public String getRefreshToken() {
        return refreshToken;
    }

    @ProtoField(number = 3, javaType =  UserDetailsProto.class)
    public UserDetailsType getUser() {
        return user;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void setUser(UserDetailsType user) {
        this.user = user;
    }
}
