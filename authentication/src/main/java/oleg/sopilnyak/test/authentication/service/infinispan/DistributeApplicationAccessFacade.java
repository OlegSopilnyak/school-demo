package oleg.sopilnyak.test.authentication.service.infinispan;

import oleg.sopilnyak.test.authentication.model.UserDetailsType;
import oleg.sopilnyak.test.authentication.service.AccessTokensStorage;
import oleg.sopilnyak.test.authentication.service.JwtService;
import oleg.sopilnyak.test.authentication.service.UserService;
import oleg.sopilnyak.test.authentication.service.impl.ApplicationAccessFacadeAdapter;
import oleg.sopilnyak.test.authentication.service.infinispan.model.AccessCredentialsProto;
import oleg.sopilnyak.test.authentication.service.infinispan.model.UserDetailsProto;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;

import org.slf4j.Logger;
import lombok.extern.slf4j.Slf4j;

/**
 * Service Implementation: service-facade for access-credentials (local storage)
 */
@Slf4j
public class DistributeApplicationAccessFacade extends ApplicationAccessFacadeAdapter {
    public DistributeApplicationAccessFacade(UserService userService, JwtService jwtService, AccessTokensStorage tokenStorage) {
        super(userService, jwtService, tokenStorage);
    }

    /**
     * To build the instance of access credentials for user-details
     *
     * @param userDetails user-details of signed-in user
     * @return valid access tokens or null, if it cannot be built
     */
    @Override
    protected AccessCredentials buildFor(final UserDetailsType userDetails) {
        return AccessCredentialsProto.of(
                jwtService.generateAccessToken(userDetails),
                jwtService.generateRefreshToken(userDetails),
                (UserDetailsProto) userDetails
        );
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
