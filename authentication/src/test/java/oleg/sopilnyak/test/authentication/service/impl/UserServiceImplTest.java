package oleg.sopilnyak.test.authentication.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.authentication.model.AccessCredentialsEntity;
import oleg.sopilnyak.test.authentication.model.UserDetailsEntity;
import oleg.sopilnyak.test.authentication.service.AccessTokensStorage;
import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.authentication.Role;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    PersistenceFacade persistenceFacade;
    @Mock
    AccessTokensStorage accessTokensStorage;
    @Spy
    @InjectMocks
    UserServiceImpl service;

    String username = "username";

    @Test
    void shouldPrepareUserDetails() {
        Long profileId = 1L;
        Long personId = 1L;
        String password = "password";
        PrincipalProfile profile = mock(PrincipalProfile.class);
        doReturn(profileId).when(profile).getId();
        doReturn(username).when(profile).getUsername();
        doReturn(true).when(profile).isPassword(password);
        doReturn(Role.SUPPORT_STAFF).when(profile).getRole();
        doReturn(Set.of(Permission.EDU_GET)).when(profile).getPermissions();
        doReturn(Optional.of(profile)).when(persistenceFacade).findPersonProfileByLogin(username);
        AuthorityPerson person = mock(AuthorityPerson.class);
        doReturn(personId).when(person).getId();
        doReturn(Optional.of(person)).when(persistenceFacade).findAuthorityPersonByProfileId(profileId);
        Set<String> authorities = Set.of("ROLE_SUPPORT_STAFF", "EDU_GET");

        UserDetailsEntity result = service.prepareUserDetails(username, password);

        // check the result
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo(username);
        assertThat(result.getPassword()).isEqualTo(password);
        assertThat(result.getId()).isEqualTo(personId);
        result.getAuthorities().forEach(granted -> assertThat(authorities).contains(granted.getAuthority()));
        // check the behavior
        verify(persistenceFacade).findPersonProfileByLogin(username);
        verify(profile).isPassword(password);
        verify(persistenceFacade).findAuthorityPersonByProfileId(profileId);
    }

    @Test
    void shouldNotPrepareUserDetails_NoProfileByUsername() {
        String password = "password";

        var result = assertThrows(Exception.class, () -> service.prepareUserDetails(username, password));

        // check the result
        assertThat(result).isNotNull().isInstanceOf(UsernameNotFoundException.class);
        // check the behavior
        verify(persistenceFacade).findPersonProfileByLogin(username);
        verify(persistenceFacade, never()).findAuthorityPersonByProfileId(anyLong());
    }

    @Test
    void shouldNotPrepareUserDetails_WrongPasswordInProfile() {
        String password = "password";
        PrincipalProfile profile = mock(PrincipalProfile.class);
        doReturn(Optional.of(profile)).when(persistenceFacade).findPersonProfileByLogin(username);

        var result = assertThrows(Exception.class, () -> service.prepareUserDetails(username, password));

        // check the result
        assertThat(result).isNotNull().isInstanceOf(SchoolAccessDeniedException.class);
        // check the behavior
        verify(persistenceFacade).findPersonProfileByLogin(username);
        verify(profile).isPassword(password);
        verify(persistenceFacade, never()).findAuthorityPersonByProfileId(anyLong());
    }

    @Test
    void shouldNotPrepareUserDetails_NoPersonForProfile() {
        Long profileId = 2L;
        String password = "password";
        PrincipalProfile profile = mock(PrincipalProfile.class);
        doReturn(profileId).when(profile).getId();
        doReturn(username).when(profile).getUsername();
        doReturn(true).when(profile).isPassword(password);
        doReturn(Role.SUPPORT_STAFF).when(profile).getRole();
        doReturn(Set.of(Permission.EDU_GET)).when(profile).getPermissions();
        doReturn(Optional.of(profile)).when(persistenceFacade).findPersonProfileByLogin(username);

        var result = assertThrows(Exception.class, () -> service.prepareUserDetails(username, password));

        // check the result
        assertThat(result).isNotNull().isInstanceOf(UsernameNotFoundException.class);
        // check the behavior
        verify(persistenceFacade).findPersonProfileByLogin(username);
        verify(profile).isPassword(password);
        verify(persistenceFacade).findAuthorityPersonByProfileId(profileId);
    }

    @Test
    void shouldLoadUserByUsername() {
        String token = "token";
        UserDetailsEntity userDetails = mock(UserDetailsEntity.class);
        AccessCredentialsEntity credentials = mock(AccessCredentialsEntity.class);
        doReturn(token).when(credentials).getToken();
        doReturn(userDetails).when(credentials).getUser();
        doReturn(Optional.of(credentials)).when(accessTokensStorage).findCredentials(username);

        var result = service.loadUserByUsername(username);

        // check the result
        assertThat(result).isNotNull().isEqualTo(userDetails);
        // check the behavior
        verify(accessTokensStorage).findCredentials(username);
        verify(accessTokensStorage).isInBlackList(token);
        verify(credentials).getToken();
        verify(credentials).getUser();
    }

    @Test
    void shouldNotLoadUserByUsername_NoStoredCredentialsByUsername() {

        var result = assertThrows(Exception.class, () -> service.loadUserByUsername(username));

        // check the result
        assertThat(result).isNotNull().isInstanceOf(UsernameNotFoundException.class);
        // check the behavior
        verify(accessTokensStorage).findCredentials(username);
        verify(accessTokensStorage, never()).isInBlackList(anyString());
    }

    @Test
    void shouldNotLoadUserByUsername_TokenInBlackList() {
        String token = "token";
        AccessCredentialsEntity credentials = mock(AccessCredentialsEntity.class);
        doReturn(token).when(credentials).getToken();
        doReturn(Optional.of(credentials)).when(accessTokensStorage).findCredentials(username);
        doReturn(true).when(accessTokensStorage).isInBlackList(token);

        var result = assertThrows(Exception.class, () -> service.loadUserByUsername(username));

        // check the result
        assertThat(result).isNotNull().isInstanceOf(UsernameNotFoundException.class);
        // check the behavior
        verify(accessTokensStorage).findCredentials(username);
        verify(accessTokensStorage).isInBlackList(token);
        verify(credentials).getToken();
        verify(credentials, never()).getUser();
    }

    @Test
    void shouldNotLoadUserByUsername_WrongTypeOfAccessCredentials() {
        String token = "token";
        AccessCredentials credentials = mock(AccessCredentials.class);
        doReturn(token).when(credentials).getToken();
        doReturn(Optional.of(credentials)).when(accessTokensStorage).findCredentials(username);

        var result = assertThrows(Exception.class, () -> service.loadUserByUsername(username));

        // check the result
        assertThat(result).isNotNull().isInstanceOf(UsernameNotFoundException.class);
        // check the behavior
        verify(accessTokensStorage).findCredentials(username);
        verify(accessTokensStorage).isInBlackList(token);
        verify(credentials).getToken();
    }
}