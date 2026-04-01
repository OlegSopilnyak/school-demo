package oleg.sopilnyak.test.authentication.service.impl;


import oleg.sopilnyak.test.authentication.service.local.model.AccessCredentialsLocalEntity;
import oleg.sopilnyak.test.authentication.model.AccessCredentialsType;
import oleg.sopilnyak.test.authentication.model.UserDetailsType;
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
import org.slf4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.ReflectionUtils;
import lombok.RequiredArgsConstructor;

/**
 * Service: Adapter of UserDetails management service for authentication
 */
@RequiredArgsConstructor
public abstract class UserServiceAdapter implements UserService {
    public static final String PROFILE_SIGNATURE_GETTER = "getSignature";
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
    public Optional<UserDetailsType> prepareUserDetails(final String username, final String password) throws UsernameNotFoundException {
        getLogger().debug("Preparing user-details by username '{}' and password...", username);
        // making signing in user-details
        return persistenceFacade.findPrincipalProfileByLogin(username).map(profile -> {
            getLogger().debug("There is the profile for user with username '{}'", username);
            if (!isPasswordValidFor(profile, password)) {
                getLogger().error("Wrong password for username: '{}'", username);
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
     * @see AccessCredentialsLocalEntity#getUser()
     */
    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        final Supplier<? extends UsernameNotFoundException> userNotFound =
                () -> new UsernameNotFoundException("User with username: '" + username + "' isn't found!");
        getLogger().debug("Loading user by username '{}'...", username);
        final AccessCredentials credentials = accessTokensStorage.findCredentials(username).orElseThrow(userNotFound);
        //
        // checking credentials of user with username
        if (accessTokensStorage.isInBlackList(credentials.getToken())) {
            getLogger().warn("Access token of user with username: '{}' is black-listed...", username);
            throw userNotFound.get();
        } else if (credentials instanceof AccessCredentialsType entity) {
            getLogger().debug("Loaded user by username '{}'", username);
            return entity.getUser();
        } else {
            getLogger().error("Wrong stored credentials entity type for username: '{}' credentials: {}", username, credentials);
            throw userNotFound.get();
        }
    }

    /**
     * To map built data to common-model type
     *
     * @param id person-id (PK of principal person)
     * @param username username of te person to sign in
     * @param password password of te person to sign in
     * @param authorities person access-permissions
     * @return the instance of user-details-type
     */
    protected abstract UserDetailsType toModel(Long id, String username, String password, Collection<? extends GrantedAuthority> authorities);

    /**
     * To get access to facade's logger
     *
     * @return concrete instance of the logger (from child class)
     * @see Logger
     */
    protected abstract Logger getLogger();

    // private methods
    // to check is password correct for the person using profile
    private boolean isPasswordValidFor(final PrincipalProfile profile, final String password) {
        final Method signatureGetter = ReflectionUtils.findMethod(profile.getClass(), PROFILE_SIGNATURE_GETTER);
        if (signatureGetter != null) {
            final String signature = (String) ReflectionUtils.invokeMethod(signatureGetter, profile);
            return !ObjectUtils.isEmpty(profile.getUsername()) && !ObjectUtils.isEmpty(signature) &&
                    isSignatureValidFor(profile, signature, password);
        }
        getLogger().warn("Not signature getter for profile class '{}'", profile.getClass());
        return false;
    }

    // to check profile's password using the signature
    private static boolean isSignatureValidFor(final PrincipalProfile profile, final String signature, final String password) {
        try {
            return signature.equals(profile.makeSignatureFor(password));
        } catch (NoSuchAlgorithmException _) {
            return false;
        }
    }

    // to make user-details for the principal profile
    private UserDetailsType makeUserDetailsFor(final PrincipalProfile profile, final String password)
            throws UsernameNotFoundException {
        final String username = profile.getUsername();
        getLogger().debug("Making user-details by username '{}' for the user's profile...", username);
        final Collection<? extends GrantedAuthority> authorities = authorities(profile);
        if (authorities.isEmpty()) {
            getLogger().error("User with username '{}' has no any authority!", username);
            throw new UsernameNotFoundException("User with username: '" + username + "' has no any authority!");
        }
        final AuthorityPerson person = persistenceFacade.findAuthorityPersonByProfileId(profile.getId())
                .orElseThrow(() -> new UsernameNotFoundException("Person with username: '" + username + "' isn't found!"));
        return toModel(person.getId(), username, password, authorities);
    }

    // to build user's authorities from principal's profile
    private Collection<? extends GrantedAuthority> authorities(final PrincipalProfile profile) {
        getLogger().debug("Loading authorities for profile with username: '{}'...", profile.getUsername());
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
