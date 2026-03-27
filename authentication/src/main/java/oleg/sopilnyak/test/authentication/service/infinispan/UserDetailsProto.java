package oleg.sopilnyak.test.authentication.service.infinispan;

import oleg.sopilnyak.test.authentication.model.UserDetailsType;

import java.util.Collection;
import java.util.List;
import org.infinispan.protostream.annotations.ProtoField;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import lombok.Getter;

/**
 * Model: ProtoEntity for school's authenticated user-details
 */
//@Proto
@Getter
//@EqualsAndHashCode(callSuper = true)
public class UserDetailsProto implements UserDetailsType {
//    private static final String AUTHORITY_ROLE_PREFIX = "ROLE_";
    @ProtoField(number = 1)
    // person id
    public Long id;
    @ProtoField(number = 2)
    // username used to authenticate the user. Cannot be null.
    public String username;
    @ProtoField(number = 3)
    // password used to authenticate the user
    public String password;
    @ProtoField(number = 4)
    // authorities granted to the user. Cannot be null.
    public Collection<String> authorityNames;

    /**
     * Returns the authorities granted to the user. Cannot return <code>null</code>.
     *
     * @return the authorities, sorted by natural key (never <code>null</code>)
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorityNames == null ? List.of() : authorityNames.stream().map(this::authority).toList();
    }

    private SimpleGrantedAuthority authority(String authorityName) {
        return new SimpleGrantedAuthority(authorityName);
    }
}
