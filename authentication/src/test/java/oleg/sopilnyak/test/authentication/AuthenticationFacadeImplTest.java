package oleg.sopilnyak.test.authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.authentication.model.AccessCredentialsType;
import oleg.sopilnyak.test.authentication.service.ApplicationAccessFacade;
import oleg.sopilnyak.test.authentication.service.local.model.UserDetailsLocalEntity;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationFacadeImplTest {
    @Mock
    ApplicationAccessFacade accessFacade;
    @Spy
    @InjectMocks
    AuthenticationFacadeImpl facade;
    final String username = "username";
    final String password = "password";
    @Mock
    UserDetailsLocalEntity userDetails;
    @Mock
    AccessCredentialsType accessCredentials;

    @Test
    void shouldSignIn_CreateNewUserDetails() {
        doReturn(userDetails).when(accessCredentials).getUser();
        doReturn(Optional.of(accessCredentials)).when(accessFacade).grantCredentialsFor(username, password);

        Optional<AccessCredentials> signed = facade.signIn(username, password);

        // check the result
        assertThat(signed).isNotNull().isNotEmpty();
        AccessCredentials credentials = signed.orElseThrow();
        assertThat(credentials).isNotNull().isInstanceOf(AccessCredentialsType.class);
        AccessCredentialsType accessCredentialsEntity = (AccessCredentialsType) credentials;
        assertThat(accessCredentialsEntity.getUser()).isSameAs(userDetails);
        // check the behavior
        verify(accessFacade).findCredentialsFor(username);
        verify(accessFacade).grantCredentialsFor(username, password);
    }

    @Test
    void shouldSignIn_StoredUserDetails() {
        doReturn(Optional.of(accessCredentials)).when(accessFacade).findCredentialsFor(username);

        Optional<AccessCredentials> signed = facade.signIn(username, password);

        // check the result
        assertThat(signed).isNotNull().isNotEmpty();
        assertThat(signed.orElseThrow()).isSameAs(accessCredentials);
        // check the behavior
        verify(accessFacade).findCredentialsFor(username);
        verify(accessFacade, never()).grantCredentialsFor(anyString(), anyString());
    }

    @Test
    void shouldNotSignIn_EmptyUserDetails() {

        Optional<AccessCredentials> signed = facade.signIn(username, password);

        // check the result
        assertThat(signed).isNotNull().isEmpty();
        // check the behavior
        verify(accessFacade).findCredentialsFor(username);
        verify(accessFacade).grantCredentialsFor(username, password);
    }

    @Test
    void shouldSignOut() {
        doReturn(Optional.of(accessCredentials)).when(accessFacade).findCredentialsFor(username);
        doReturn(Optional.of(accessCredentials)).when(accessFacade).revokeCredentialsFor(username);

        Optional<AccessCredentials> signed = facade.signOut(username);

        // check the result
        assertThat(signed).isNotNull().isNotEmpty();
        assertThat(signed.orElseThrow()).isSameAs(accessCredentials);
        // check the behavior
        verify(accessFacade).findCredentialsFor(username);
        verify(accessFacade).revokeCredentialsFor(username);
    }

    @Test
    void shouldNotSignOut_NoStoredAccessCredentials() {

        Optional<AccessCredentials> signed = facade.signOut(username);

        // check the result
        assertThat(signed).isNotNull().isEmpty();
        // check the behavior
        verify(accessFacade).findCredentialsFor(username);
        verify(accessFacade, never()).revokeCredentialsFor(anyString());
    }

    @Test
    void shouldRefreshToken() {
        String refreshToken = "refresh-token";
        doReturn(Optional.of(accessCredentials)).when(accessFacade).refreshCredentialsFor(username, refreshToken);

        Optional<AccessCredentials> fresh = facade.refresh(refreshToken, username);

        // check the result
        assertThat(fresh).isNotNull().isNotEmpty();
        assertThat(fresh.orElseThrow()).isEqualTo(accessCredentials);
        // check the behavior
        verify(accessFacade).refreshCredentialsFor(username, refreshToken);
    }

    @Test
    void shouldNotRefreshToken_NoStoredAccessCredentials() {
        String refreshToken = "refresh-token";

        Optional<AccessCredentials> fresh = facade.refresh(refreshToken, username);

        // check the result
        assertThat(fresh).isNotNull().isEmpty();
        // check the behavior
        verify(accessFacade).refreshCredentialsFor(username, refreshToken);
    }
}