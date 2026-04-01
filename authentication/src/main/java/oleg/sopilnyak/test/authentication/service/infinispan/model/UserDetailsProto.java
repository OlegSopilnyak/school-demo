package oleg.sopilnyak.test.authentication.service.infinispan.model;

import oleg.sopilnyak.test.authentication.model.UserDetailsType;

import java.util.Collection;
import java.util.List;
import org.infinispan.protostream.annotations.ProtoField;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Model:  Protobuf Entity for school's authenticated user-details
 */
public class UserDetailsProto implements UserDetailsType {
//    private static final String AUTHORITY_ROLE_PREFIX = "ROLE_";
    // person id
    private Long id;
    // username used to authenticate the user. Cannot be null.
    private String username;
    // password used to authenticate the user
    private String password;
    // authorities granted to the user. Cannot be null.
    private Collection<String> authorityNames;

    @ProtoField(number = 1)
    public Long getId() {
        return id;
    }

    @ProtoField(number = 2)
    public String getUsername() {
        return username;
    }

    @ProtoField(number = 3)
    public String getPassword() {
        return password;
    }

    @ProtoField(number = 4)
    public Collection<String> getAuthorityNames() {
        return authorityNames;
    }

    /**
     * Returns the authorities granted to the user. Cannot return <code>null</code>.
     *
     * @return the authorities, sorted by natural key (never <code>null</code>)
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorityNames == null ? List.of() : authorityNames.stream().map(this::authority).toList();
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAuthorityNames(Collection<String> authorityNames) {
        this.authorityNames = authorityNames;
    }

    private SimpleGrantedAuthority authority(String authorityName) {
        return new SimpleGrantedAuthority(authorityName);
    }
}
