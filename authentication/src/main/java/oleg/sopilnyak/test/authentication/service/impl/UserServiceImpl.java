package oleg.sopilnyak.test.authentication.service.impl;


import oleg.sopilnyak.test.authentication.model.UserDetailsEntity;
import oleg.sopilnyak.test.authentication.service.UserService;
import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;

import java.util.Collection;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    // principal person profile persistence facade
    private final ProfilePersistenceFacade profilePersistenceFacade;

    /**
     * To make userdetails for user, using username and password
     *
     * @param username the username identifying the user whose data is required.
     * @param password the value
     * @return user-details instance
     * @see ProfilePersistenceFacade#findPersonProfileByLogin(String)
     */
    @Override
    public UserDetails prepareUserDetails(final String username, final String password) throws UsernameNotFoundException {
        log.debug("Loading user by username '{}' and password...", username);
        // making signing in user-details
        final PrincipalProfile profile = profilePersistenceFacade.findPersonProfileByLogin(username).map(PrincipalProfile.class::cast)
                .orElseThrow(() -> new UsernameNotFoundException("Profile with username: '" + username + "' isn't found!"));
        if (!profile.isPassword(password)) {
            log.error("Wrong password for username: '{}'", username);
            throw new SchoolAccessDeniedException("Wrong password for username: " + username);
        }
        log.debug("Password for person with username '{}' is correct", username);
        return makeUserDetailsFor(profile);
    }

    /**
     * Locates the user based on the username. In the actual implementation, the search
     * may, possibly be case-sensitive, or case-insensitive depending on how the
     * implementation instance is configured. In this case, the <code>UserDetails</code>
     * object that comes back may have a username that is of a different case than what
     * was actually requested...
     *
     * @param username the username identifying the user whose data is required.
     * @return a fully populated user record (never <code>null</code>)
     * @throws UsernameNotFoundException if the user could not be found or the user has no
     *                                   GrantedAuthority
     * @see ProfilePersistenceFacade#findPersonProfileByLogin(String)
     */
    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        log.debug("Loading user by username '{}'...", username);
        final PrincipalProfile profile = profilePersistenceFacade.findPersonProfileByLogin(username).map(PrincipalProfile.class::cast)
                .orElseThrow(() -> new UsernameNotFoundException("Profile with username: '" + username + "' isn't found!"));
        log.debug("Loaded user by username '{}'", username);
        return makeUserDetailsFor(profile);
    }

    // private methods
    // to make user-details for the principal profile
    private UserDetails makeUserDetailsFor(final PrincipalProfile profile) throws UsernameNotFoundException {
        String username = profile.getLogin();
        final Collection<? extends GrantedAuthority> authorities = authorities(profile);
        if (authorities.isEmpty()) {
            log.error("User with username '{}' has no any authority!", username);
            throw new UsernameNotFoundException("User with username: '" + username + "' has no any authority!");
        }
        return new UserDetailsEntity(username, "null", authorities);
    }

    // to get user's authorities from principal's profile
    private Collection<? extends GrantedAuthority> authorities(final PrincipalProfile profile) {
        log.debug("Loading authorities for user '{}'...", profile.getLogin());
        return Set.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
