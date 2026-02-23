package oleg.sopilnyak.test.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.authentication.model.AccessCredentialsEntity;
import oleg.sopilnyak.test.authentication.model.UserDetailsEntity;
import oleg.sopilnyak.test.authentication.service.AccessTokensStorage;
import oleg.sopilnyak.test.authentication.service.JwtService;
import oleg.sopilnyak.test.authentication.service.UserService;
import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

@ExtendWith(MockitoExtension.class)
class AuthenticationFacadeImplTest {
    @Mock
    UserService userService;
    @Mock
    JwtService jwtService;
    @Mock
    AccessTokensStorage tokenStorage;

    @Spy
    @InjectMocks
    AuthenticationFacadeImpl facade;
    final String username = "username";
    final String password = "password";
    final String activeToken = "active-token";
    @Mock
    UserDetailsEntity userDetails;

    @Test
    void shouldSignIn_CreateNewUserDetails() {
        doReturn(Optional.of(userDetails)).when(userService).prepareUserDetails(username, password);

        Optional<AccessCredentials> signed = facade.signIn(username, password);

        // check the result
        assertThat(signed).isNotNull().isNotEmpty();
        AccessCredentials accessCredentials = signed.orElseThrow();
        assertThat(accessCredentials).isNotNull().isInstanceOf(AccessCredentialsEntity.class);
        AccessCredentialsEntity accessCredentialsEntity = (AccessCredentialsEntity) accessCredentials;
        assertThat(accessCredentialsEntity.getUser()).isSameAs(userDetails);
        // check the behavior
        verify(tokenStorage).findCredentials(username);
        verify(jwtService).generateAccessToken(userDetails);
        verify(jwtService).generateRefreshToken(userDetails);
        verify(tokenStorage).storeFor(username, accessCredentials);
    }

    @Test
    void shouldSignIn_StoredUserDetails() {
        AccessCredentialsEntity entity = AccessCredentialsEntity.builder().user(userDetails).build();
        doReturn(Optional.of(entity)).when(tokenStorage).findCredentials(username);

        Optional<AccessCredentials> signed = facade.signIn(username, password);

        // check the result
        assertThat(signed).isNotNull().isNotEmpty();
        assertThat(signed.orElseThrow()).isSameAs(entity);
        // check the behavior
        verify(tokenStorage).findCredentials(username);
        verify(jwtService, never()).generateAccessToken(any(UserDetails.class));
        verify(jwtService, never()).generateRefreshToken(any(UserDetails.class));
    }

    @Test
    void shouldNotSignIn_EmptyUserDetails() {

        Optional<AccessCredentials> signed = facade.signIn(username, password);

        // check the result
        assertThat(signed).isNotNull().isEmpty();
        // check the behavior
        verify(tokenStorage).findCredentials(username);
        verify(jwtService, never()).generateAccessToken(any(UserDetails.class));
        verify(jwtService, never()).generateRefreshToken(any(UserDetails.class));
    }

    @Test
    void shouldSignOut() {
        AccessCredentialsEntity entity = AccessCredentialsEntity.builder().user(userDetails).token(activeToken).build();
        doReturn(Optional.of(entity)).when(tokenStorage).findCredentials(username);
        doReturn(username).when(jwtService).extractUserName(activeToken);

        Optional<AccessCredentials> signed = facade.signOut(activeToken);

        // check the result
        assertThat(signed).isNotNull().isNotEmpty();
        assertThat(signed.orElseThrow()).isSameAs(entity);
        // check the behavior
        verify(jwtService).extractUserName(activeToken);
        verify(tokenStorage).findCredentials(username);
        verify(tokenStorage).toBlackList(activeToken);
        verify(tokenStorage).deleteCredentials(username);
    }

    @Test
    void shouldNotSignOut_NoStoredAccessCredentials() {
        doReturn(username).when(jwtService).extractUserName(activeToken);

        Optional<AccessCredentials> signed = facade.signOut(activeToken);

        // check the result
        assertThat(signed).isNotNull().isEmpty();
        // check the behavior
        verify(jwtService).extractUserName(activeToken);
        verify(tokenStorage).findCredentials(username);
        verify(tokenStorage, never()).toBlackList(anyString());
        verify(tokenStorage, never()).deleteCredentials(anyString());
    }

    @Test
    void shouldFindCredentialsFor() {
        AccessCredentialsEntity entity = AccessCredentialsEntity.builder().user(userDetails).token(activeToken).build();
        doReturn(Optional.of(entity)).when(tokenStorage).findCredentials(username);

        Optional<AccessCredentials> signed = facade.findCredentialsFor(username);

        // check the result
        assertThat(signed).isNotNull().isNotEmpty();
        assertThat(signed.orElseThrow()).isSameAs(entity);
        // check the behavior
        verify(tokenStorage).findCredentials(username);
    }

    @Test
    void shouldNotFindCredentialsFor_NoStoredAccessCredentials() {

        Optional<AccessCredentials> signed = facade.findCredentialsFor(username);

        // check the result
        assertThat(signed).isNotNull().isEmpty();
        // check the behavior
        verify(tokenStorage).findCredentials(username);
    }

    @Test
    void shouldRefreshToken() {
        String refreshToken = "refresh-token";
        doReturn(username).when(jwtService).extractUserName(refreshToken);
        doReturn(username).when(userDetails).getUsername();
        doReturn(password).when(userDetails).getPassword();
        AccessCredentialsEntity entity = AccessCredentialsEntity.builder().user(userDetails).build();
        doReturn(Optional.of(entity)).when(tokenStorage).findCredentials(username);
        doReturn(Optional.of(userDetails)).when(userService).prepareUserDetails(username, password);

        Optional<AccessCredentials> fresh = facade.refresh(refreshToken);

        // check the result
        assertThat(fresh).isNotNull().isNotEmpty();
        assertThat(fresh.orElseThrow()).isEqualTo(entity);
        // check the behavior
        verify(jwtService).extractUserName(refreshToken);
        verify(tokenStorage).findCredentials(username);
        verify(userService).prepareUserDetails(username, password);
    }

    @Test
    void shouldNotRefreshToken_NoStoredAccessCredentials() {
        String refreshToken = "refresh-token";
        doReturn(username).when(jwtService).extractUserName(refreshToken);

        var fresh = assertThrows(Exception.class, () -> facade.refresh(refreshToken));

        // check the result
        assertThat(fresh).isNotNull().isInstanceOf(SchoolAccessDeniedException.class);
        // check the behavior
        verify(jwtService).extractUserName(refreshToken);
        verify(tokenStorage).findCredentials(username);
        verify(userService, never()).prepareUserDetails(anyString(), anyString());
    }

    @Test
    void shouldNotRefreshToken_WrongAccessCredentialsType() {
        String refreshToken = "refresh-token";
        doReturn(username).when(jwtService).extractUserName(refreshToken);
        // not AccessCredentialsEntity type of credentials
        AccessCredentials credentials = mock(AccessCredentials.class);
        doReturn(Optional.of(credentials)).when(tokenStorage).findCredentials(username);

        var fresh = assertThrows(Exception.class, () -> facade.refresh(refreshToken));

        // check the result
        assertThat(fresh).isNotNull().isInstanceOf(SchoolAccessDeniedException.class);
        // check the behavior
        verify(jwtService).extractUserName(refreshToken);
        verify(tokenStorage).findCredentials(username);
        verify(userService, never()).prepareUserDetails(anyString(), anyString());
    }
}