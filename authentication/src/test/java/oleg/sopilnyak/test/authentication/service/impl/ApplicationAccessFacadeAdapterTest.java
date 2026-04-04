package oleg.sopilnyak.test.authentication.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.authentication.model.AccessCredentialsType;
import oleg.sopilnyak.test.authentication.model.UserDetailsType;
import oleg.sopilnyak.test.authentication.service.AccessTokensStorage;
import oleg.sopilnyak.test.authentication.service.JwtService;
import oleg.sopilnyak.test.authentication.service.UserService;
import oleg.sopilnyak.test.authentication.service.local.model.AccessCredentialsLocalEntity;
import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ApplicationAccessFacadeAdapterTest {
    @Mock
    UserService userService;
    @Mock
    JwtService jwtService;
    @Mock
    AccessTokensStorage tokenStorage;

    ApplicationAccessFacadeAdapter facade;
    @Mock
    AccessCredentialsLocalEntity accessCredentials;
    @Mock
    UserDetailsType userDetails;

    @BeforeEach
    void setUp() {
        facade = spy(new TestApplicationAccessFacade(userService, jwtService, tokenStorage));
    }

    @Test
    void shouldFindCredentialsFor() {
        // preparing test data
        String username = "username";
        doReturn(Optional.of(accessCredentials)).when(tokenStorage).findCredentials(username);

        // acting
        Optional<AccessCredentials> result = facade.findCredentialsFor(username);

        // check the result
        assertThat(result).isNotNull().isNotEmpty();
        assertThat(result.orElseThrow()).isSameAs(accessCredentials);
        // check the behavior
        verify(tokenStorage).findCredentials(username);
    }

    @Test
    void shouldGrantCredentialsForUsernamePassword() {
        // preparing test data
        String username = "username";
        String password = "password";
        doReturn(userDetails).when(accessCredentials).getUser();
        doReturn(Optional.of(userDetails)).when(userService).prepareUserDetails(username, password);
        doReturn(Optional.of(accessCredentials)).when(facade).grantCredentialsFor(userDetails);

        // acting
        Optional<AccessCredentials> result = facade.grantCredentialsFor(username, password);

        // check the result
        assertThat(result).isNotNull().isNotEmpty();
        assertThat(result.orElseThrow()).isInstanceOf(AccessCredentialsType.class);
        AccessCredentialsType entity = (AccessCredentialsType) result.orElseThrow();
        assertThat(entity.getUser()).isNotNull().isSameAs(userDetails);
        // check the behavior
        verify(userService).prepareUserDetails(username, password);
        verify(facade).grantCredentialsFor(userDetails);
    }

    @Test
    void shouldGrantCredentialsForUserDetails() {
        // preparing test data

        // acting
        Optional<AccessCredentials> result = facade.grantCredentialsFor(userDetails);

        // check the result
        assertThat(result).isNotNull().isNotEmpty();
        assertThat(result.orElseThrow()).isInstanceOf(AccessCredentialsType.class);
        AccessCredentialsType entity = (AccessCredentialsType) result.orElseThrow();
        assertThat(entity).isNotSameAs(accessCredentials);
        assertThat(entity.getUser()).isNotNull().isSameAs(userDetails);
        // check the behavior
        verify(facade).buildFor(userDetails);
    }

    @Test
    void shouldRevokeCredentialsFor() {
        // preparing test data
        String username = "username";
        String token = "username-token";
        doReturn(token).when(accessCredentials).getToken();
        doReturn(Optional.of(accessCredentials)).when(tokenStorage).findCredentials(username);

        // acting
        Optional<AccessCredentials> result = facade.revokeCredentialsFor(username);

        // check the result
        assertThat(result).isNotNull().isNotEmpty();
        assertThat(result.orElseThrow()).isInstanceOf(AccessCredentialsType.class);
        AccessCredentialsType entity = (AccessCredentialsType) result.orElseThrow();
        assertThat(entity).isSameAs(accessCredentials);
        // check the behavior
        verify(tokenStorage).findCredentials(username);
        verify(tokenStorage).toBlackList(token);
        verify(tokenStorage).deleteCredentials(username);
    }

    @Test
    void shouldRefreshCredentialsFor() {
        // preparing test data
        String username = "username";
        String token = "username-token";
        doReturn(username).when(jwtService).extractUserName(token);
        doReturn(userDetails).when(accessCredentials).getUser();
        doReturn(Optional.of(accessCredentials)).when(tokenStorage).findCredentials(username);

        // acting
        Optional<AccessCredentials> result = facade.refreshCredentialsFor(username, token);

        // check the result
        assertThat(result).isNotNull().isNotEmpty();
        assertThat(result.orElseThrow()).isInstanceOf(AccessCredentialsType.class);
        AccessCredentialsType entity = (AccessCredentialsType) result.orElseThrow();
        assertThat(entity).isNotSameAs(accessCredentials);
        assertThat(entity.getUser()).isNotNull().isSameAs(userDetails);
        // check the behavior
        verify(jwtService).isTokenExpired(token);
        verify(tokenStorage).findCredentials(username);
        verify(jwtService).extractUserName(token);
        verify(facade).buildFor(userDetails);
    }

    @Test
    void shouldNotRefreshCredentialsFor_ExpiredToken() {
        // preparing test data
        String username = "username";
        String token = "username-token";
        doReturn(true).when(jwtService).isTokenExpired(token);

        // acting
        Optional<AccessCredentials> result = facade.refreshCredentialsFor(username, token);

        // check the result
        assertThat(result).isNotNull().isEmpty();
        // check the behavior
        verify(jwtService).isTokenExpired(token);
        verify(tokenStorage).deleteCredentialsWithRefreshToken(token);
        verify(tokenStorage, never()).findCredentials(anyString());
    }

    @Test
    void shouldRefreshTokenIsExpired() {
        // preparing test data
        String token = "username-token";

        // acting
        Optional<AccessCredentials> result = ReflectionTestUtils.invokeMethod(facade, "refreshTokenIsExpired", token);

        // check the result
        assertThat(result).isNotNull().isEmpty();
        // check the behavior
        verify(tokenStorage).deleteCredentialsWithRefreshToken(token);
    }

    @Test
    void shouldRefreshedCredentials() {
        // preparing test data
        String username = "username";
        String token = "username-token";
        doReturn(username).when(jwtService).extractUserName(token);
        doReturn(userDetails).when(accessCredentials).getUser();
        doReturn(Optional.of(accessCredentials)).when(tokenStorage).findCredentials(username);

        // acting
        Optional<AccessCredentials> result = ReflectionTestUtils.invokeMethod(facade, "refreshedCredentials", username, token);

        // check the result
        assertThat(result).isNotNull().isNotEmpty();
        assertThat(result.orElseThrow()).isInstanceOf(AccessCredentialsType.class);
        AccessCredentialsType entity = (AccessCredentialsType) result.orElseThrow();
        assertThat(entity).isNotSameAs(accessCredentials);
        assertThat(entity.getUser()).isNotNull().isSameAs(userDetails);
        // check the behavior
        verify(tokenStorage).findCredentials(username);
        verify(jwtService).extractUserName(token);
        verify(facade).buildFor(userDetails);
    }

    @Test
    void shouldExtractUserName() {
        // preparing test data
        String username = "username";
        String token = "username-token";
        doReturn(username).when(jwtService).extractUserName(token);

        // acting
        String result = ReflectionTestUtils.invokeMethod(facade, "extractUserName", username, token);

        // check the result
        assertThat(result).isSameAs(username);
        // check the behavior
        verify(jwtService).extractUserName(token);
    }

    @Test
    void shouldNotExtractUserName_WrongActiveUsername() {
        // preparing test data
        String correctUsername = "username";
        String wrongActiveUsername = "user.name";
        String token = "username-token";
        doReturn(correctUsername).when(jwtService).extractUserName(token);

        // acting
        Exception error = assertThrows(RuntimeException.class, () ->
                ReflectionTestUtils.invokeMethod(facade, "extractUserName", wrongActiveUsername, token)
        );

        // check the result
        assertThat(error).isInstanceOf(SchoolAccessDeniedException.class);
        assertThat(error.getMessage()).isEqualTo("Person with username: '" + correctUsername + "' isn't signed in");
        // check the behavior
        verify(jwtService).extractUserName(token);
    }

    @Test
    void shouldRebuildCredentialsFor() {
        // preparing test data
        String username = "username";
        doReturn(userDetails).when(accessCredentials).getUser();
        doReturn(Optional.of(accessCredentials)).when(facade).grantCredentialsFor(userDetails);

        // acting
        Optional<AccessCredentials> result = ReflectionTestUtils.invokeMethod(facade, "rebuildCredentialsFor", accessCredentials, username);

        // check the result
        assertThat(result).isNotNull().isNotEmpty();
        assertThat(result.orElseThrow()).isInstanceOf(AccessCredentialsType.class);
        AccessCredentialsType entity = (AccessCredentialsType) result.orElseThrow();
        assertThat(entity).isSameAs(accessCredentials);
        // check the behavior
        verify(facade).grantCredentialsFor(userDetails);
    }

    @Test
    void shouldNotRebuildCredentialsFor_WrongAccessCredentialsType() {
        // preparing test data
        String username = "username";
        AccessCredentials wrongTypeCredentials = mock(AccessCredentials.class);

        // acting
        Exception error = assertThrows(RuntimeException.class, () ->
                ReflectionTestUtils.invokeMethod(facade, "rebuildCredentialsFor", wrongTypeCredentials, username)
        );

        // check the result
        assertThat(error).isInstanceOf(SchoolAccessDeniedException.class);
        assertThat(error.getMessage()).isEqualTo("Person with username: '" + username + "' isn't signed in");
        // check the behavior
        verify(facade, never()).grantCredentialsFor(any(UserDetailsType.class));
    }

    // private classes
    private static  class TestApplicationAccessFacade extends ApplicationAccessFacadeAdapter {

        public TestApplicationAccessFacade(UserService userService, JwtService jwtService, AccessTokensStorage tokenStorage) {
            super(userService, jwtService, tokenStorage);
        }

        @Override
        protected AccessCredentials buildFor(UserDetailsType userDetails) {
            AccessCredentialsType built = mock(AccessCredentialsType.class);
            doReturn(userDetails).when(built).getUser();
            return built;
        }

        @Override
        protected Logger getLogger() {
            return mock(Logger.class);
        }
    }
}
