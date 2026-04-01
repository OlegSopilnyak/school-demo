package oleg.sopilnyak.test.authentication.service.local;

import oleg.sopilnyak.test.authentication.model.UserDetailsType;
import oleg.sopilnyak.test.authentication.service.AccessTokensStorage;
import oleg.sopilnyak.test.authentication.service.impl.UserServiceAdapter;
import oleg.sopilnyak.test.authentication.service.local.model.UserDetailsLocalEntity;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;

import java.util.Collection;
import org.slf4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import lombok.extern.slf4j.Slf4j;

/**
 * Service: Implementation of UserDetails management service for authentication (local data-model)
 */
@Slf4j
public class LocalUserService extends UserServiceAdapter {
    public LocalUserService(PersistenceFacade persistenceFacade, AccessTokensStorage accessTokensStorage) {
        super(persistenceFacade, accessTokensStorage);
    }

    /**
     * To map built data to common-model type
     *
     * @param id          person-id (PK of principal person)
     * @param username    username of te person to sign in
     * @param password    password of te person to sign in
     * @param authorities person access-permissions
     * @return the instance of user-details-type
     */
    @Override
    protected UserDetailsType toModel(Long id, String username, String password, Collection<? extends GrantedAuthority> authorities) {
        return new UserDetailsLocalEntity(id, username, password, authorities);
    }

    /**
     * To get access to facade's logger
     *
     * @return concrete instance of the logger (from child class)
     * @see Logger
     */
    @Override
    protected Logger getLogger() {
        return log;
    }
}
