package oleg.sopilnyak.test.authentication.service.infinispan;

import oleg.sopilnyak.test.authentication.model.UserDetailsType;
import oleg.sopilnyak.test.authentication.service.AccessTokensStorage;
import oleg.sopilnyak.test.authentication.service.impl.UserServiceAdapter;
import oleg.sopilnyak.test.authentication.service.infinispan.model.UserDetailsProto;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;

import java.util.Collection;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.springframework.security.core.GrantedAuthority;
import lombok.extern.slf4j.Slf4j;

/**
 * Service: Implementation of UserDetails management service for authentication (distribute data-model)
 */
@Slf4j
public class DistributeUserService extends UserServiceAdapter {
    public DistributeUserService(PersistenceFacade persistenceFacade, AccessTokensStorage accessTokensStorage) {
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
    protected UserDetailsType toModel(
            final Long id, final String username, final String password,
            final Collection<? extends GrantedAuthority> authorities
    ) {
        return UserDetailsProto.builder()
                .id(id).username(username).password(password)
                .authorityNames(authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet()))
                .build();
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
