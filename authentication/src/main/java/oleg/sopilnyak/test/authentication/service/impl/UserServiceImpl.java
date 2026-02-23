package oleg.sopilnyak.test.authentication.service.impl;


import oleg.sopilnyak.test.authentication.model.AccessCredentialsEntity;
import oleg.sopilnyak.test.authentication.model.UserDetailsEntity;
import oleg.sopilnyak.test.authentication.service.AccessTokensStorage;
import oleg.sopilnyak.test.authentication.service.UserService;
import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;

import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    // principal person profile persistence facade
    private final PersistenceFacade persistenceFacade;
    // the storage of active tokes and users
    private final AccessTokensStorage accessTokensStorage;

    /**
     * To make userdetails for user, using username and password
     *
     * @param username the username identifying the user whose data is required.
     * @param password the value
     * @return user-details instance
     * @see ProfilePersistenceFacade#findPersonProfileByLogin(String)
     */
    @Override
    public Optional<UserDetailsEntity> prepareUserDetails(final String username, final String password) throws UsernameNotFoundException {
        log.debug("Loading user by username '{}' and password...", username);
        // making signing in user-details
        final Optional<PrincipalProfile> foundProfile = persistenceFacade.findPersonProfileByLogin(username).map(PrincipalProfile.class::cast);
        return foundProfile.map(profile -> {
            log.debug("There is the profile for user with username '{}'", username);
            if (!isPasswordValidFor(profile, password)) {
                log.error("Wrong password for username: '{}'", username);
                throw new SchoolAccessDeniedException("Wrong password for username: " + username);
            }
            return makeUserDetailsFor(profile, password);
        });
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
     * @see AccessTokensStorage#findCredentials(String)
     * @see AccessCredentialsEntity#getUser()
     */
    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        final Supplier<? extends UsernameNotFoundException> userNotFound =
                () -> new UsernameNotFoundException("User with username: '" + username + "' isn't found!");
        log.debug("Loading user by username '{}'...", username);
        final AccessCredentials credentials = accessTokensStorage.findCredentials(username)
                .orElseThrow(userNotFound);
        //
        // checking credentials of user with username
        if (accessTokensStorage.isInBlackList(credentials.getToken())) {
            log.warn("Access token of user with username: '{}' is black-listed...", username);
            throw userNotFound.get();
        } else if (credentials instanceof AccessCredentialsEntity entity) {
            log.debug("Loaded user by username '{}'", username);
            return entity.getUser();
        } else {
            log.error("Wrong stored credentials entity type for username: '{}' credentials: {}", username, credentials);
            throw userNotFound.get();
        }
    }

    // private methods
    // to check is password correct for the person using profile
    private boolean isPasswordValidFor(final PrincipalProfile profile, final String password) {
        final Method signatureGetter = ReflectionUtils.findMethod(profile.getClass(), "getSignature");
        if (signatureGetter == null) {
            return false;
        }
        final String username = profile.getUsername();
        final String signature = (String) ReflectionUtils.invokeMethod(signatureGetter, profile);
        if (ObjectUtils.isEmpty(signature) || ObjectUtils.isEmpty(username)) {
            return false;
        }
        try {
            return signature.equals(profile.makeSignatureFor(password));
        } catch (NoSuchAlgorithmException _) {
            return false;
        }
    }

    // to make user-details for the principal profile
    private UserDetailsEntity makeUserDetailsFor(final PrincipalProfile profile, final String password)
            throws UsernameNotFoundException {
        final String username = profile.getUsername();
        log.debug("Making user-details by username '{}' for the user's profile...", username);
        final Collection<? extends GrantedAuthority> authorities = authorities(profile);
        if (authorities.isEmpty()) {
            log.error("User with username '{}' has no any authority!", username);
            throw new UsernameNotFoundException("User with username: '" + username + "' has no any authority!");
        }
        final AuthorityPerson person = persistenceFacade.findAuthorityPersonByProfileId(profile.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Person with username: '" + username + "' isn't found!"));
        return new UserDetailsEntity(person.getId(), username, password, authorities);
    }

    // to get user's authorities from principal's profile
    private Collection<? extends GrantedAuthority> authorities(final PrincipalProfile profile) {
        log.debug("Loading authorities for profile with username: '{}'...", profile.getUsername());
        final Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + validAuthority(profile.getRole().name())));
        profile.getPermissions().stream().map(Enum::name)
                .map(permission -> new SimpleGrantedAuthority(validAuthority(permission)))
                .forEach(authorities::add);
        return authorities;
    }

    private static String validAuthority(final String authority) {
        Assert.isTrue(!authority.startsWith("ROLE_"),
                () -> authority + " cannot start with ROLE_ (it is automatically added for role's value)");
        return authority;
    }

}
