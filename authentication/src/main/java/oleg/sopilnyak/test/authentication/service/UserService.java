package oleg.sopilnyak.test.authentication.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * Service: UserDetails service for authentication
 */
public interface UserService extends UserDetailsService {
    /**
     * To make userdetails for user, using username and password
     *
     * @param username the value
     * @param password the value
     * @return user-details instance
     */
    UserDetails prepareUserDetails(String username, String password);
}
