package oleg.sopilnyak.test.authentication.service.local;

import oleg.sopilnyak.test.authentication.model.UserDetailsType;
import oleg.sopilnyak.test.authentication.service.AccessTokensStorage;
import oleg.sopilnyak.test.authentication.service.JwtService;
import oleg.sopilnyak.test.authentication.service.UserService;
import oleg.sopilnyak.test.authentication.service.impl.ApplicationAccessFacadeAdapter;
import oleg.sopilnyak.test.authentication.service.local.model.AccessCredentialsLocalEntity;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;

import org.slf4j.Logger;
import lombok.extern.slf4j.Slf4j;

/**
 * Service Implementation: service-facade for access-credentials (local storage)
 */
@Slf4j
public class LocalApplicationAccessFacade extends ApplicationAccessFacadeAdapter {
    public LocalApplicationAccessFacade(UserService userService, JwtService jwtService, AccessTokensStorage tokenStorage) {
        super(userService, jwtService, tokenStorage);
    }

    /**
     * To build the instance of access credentials for user-details
     *
     * @param userDetails user-details of signed-in user
     * @return valid access tokens or null, if it cannot be built
     */
    @Override
    protected AccessCredentials buildFor(UserDetailsType userDetails) {
        return AccessCredentialsLocalEntity.builder()
                .user(userDetails)
                .token(jwtService.generateAccessToken(userDetails))
                .refreshToken(jwtService.generateRefreshToken(userDetails))
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
