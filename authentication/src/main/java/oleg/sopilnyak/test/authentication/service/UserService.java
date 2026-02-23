package oleg.sopilnyak.test.authentication.service;

import oleg.sopilnyak.test.authentication.model.UserDetailsEntity;

import java.util.Optional;
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
     * @return user-details instance or empty
     * @see UserDetailsEntity
     * @see Optional
     */
    Optional<UserDetailsEntity> prepareUserDetails(String username, String password);
}
