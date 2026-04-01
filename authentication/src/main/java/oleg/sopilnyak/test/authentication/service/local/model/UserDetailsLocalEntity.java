package oleg.sopilnyak.test.authentication.service.local.model;

import oleg.sopilnyak.test.authentication.model.UserDetailsType;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Model: Entity for school's authenticated user-details
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class UserDetailsLocalEntity extends User implements UserDetailsType {
    // person id
    private final Long id;

    /**
     * Calls the more complex constructor with all boolean arguments set to {@code true}.
     *
     * @param username    the username presented to the
     *                    <code>DaoAuthenticationProvider</code>
     * @param password    the password that should be presented to the
     *                    <code>DaoAuthenticationProvider</code>
     * @param authorities the authorities that should be granted to the caller if they
     *                    presented the correct username and password and the user is enabled. Not null.
     */
    public UserDetailsLocalEntity(Long id, String username, String password, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
    }
}
