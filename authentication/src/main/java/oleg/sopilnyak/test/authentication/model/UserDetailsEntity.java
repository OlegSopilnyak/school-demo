package oleg.sopilnyak.test.authentication.model;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Model: Entity for school's authenticated user-details
 */
@Getter
@EqualsAndHashCode(callSuper = true)
public class UserDetailsEntity extends User implements UserDetails {
    private final Long id;
    /**
     * Calls the more complex constructor with all boolean arguments set to {@code true}.
     *
     * @param username              the username presented to the
     *                              <code>DaoAuthenticationProvider</code>
     * @param password              the password that should be presented to the
     *                              <code>DaoAuthenticationProvider</code>
     * @param authorities           the authorities that should be granted to the caller if they
     *                              presented the correct username and password and the user is enabled. Not null.
     */
    public UserDetailsEntity(Long id, String username, String password, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.id = id;
    }
}
