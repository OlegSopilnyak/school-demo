package oleg.sopilnyak.test.authentication.service.infinispan.model;

import oleg.sopilnyak.test.authentication.model.UserDetailsType;

import java.util.Collection;
import java.util.List;
import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Setter;

/**
 * Model:  Protobuf Entity for school's authenticated user-details
 */
@Setter
@Indexed
@EqualsAndHashCode
public class UserDetailsProto implements UserDetailsType {
    // person id
    private Long id;
    // username used to authenticate the user. Cannot be null.
    private String username;
    // password used to authenticate the user
    private String password;
    // authorities granted to the user. Cannot be null.
    private Collection<String> authorityNames;

    @Builder
    @ProtoFactory
    public static UserDetailsProto of(Long id, String username, String password, Collection<String> authorityNames) {
        return new UserDetailsProto(id, username, password, authorityNames);
    }

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

    // private methods
    private SimpleGrantedAuthority authority(String authorityName) {
        return new SimpleGrantedAuthority(authorityName);
    }

    private UserDetailsProto(Long id, String username, String password, Collection<String> authorityNames) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.authorityNames = authorityNames;
    }
}
