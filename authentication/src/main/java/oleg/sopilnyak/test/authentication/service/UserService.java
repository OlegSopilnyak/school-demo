package oleg.sopilnyak.test.authentication.service;

import oleg.sopilnyak.test.authentication.model.UserDetailsEntity;

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
     * @see UserDetailsEntity
     */
    UserDetailsEntity prepareUserDetails(String username, String password);
}
