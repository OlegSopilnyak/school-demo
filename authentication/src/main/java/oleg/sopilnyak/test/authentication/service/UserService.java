package oleg.sopilnyak.test.authentication.service;

import oleg.sopilnyak.test.authentication.model.UserDetailsType;
import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;

import java.util.Optional;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Service: Facade of UserDetails management service for authentication
 */
public interface UserService extends UserDetailsService {
    /**
     * To make userdetails for user, using username and password
     *
     * @param username the value
     * @param password the value
     * @return user-details instance or empty
     * @see PrincipalProfile#getUsername()
     * @see UserDetailsType
     * @see Optional
     * @throws UsernameNotFoundException unknown username (no person profile)
     * @throws SchoolAccessDeniedException access denied for the person
     */
    Optional<UserDetailsType> prepareUserDetails(String username, String password)
            throws SchoolAccessDeniedException, UsernameNotFoundException;
}
