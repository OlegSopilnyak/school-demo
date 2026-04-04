package oleg.sopilnyak.test.authentication.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.authentication.model.AccessCredentialsType;
import oleg.sopilnyak.test.authentication.model.UserDetailsType;
import oleg.sopilnyak.test.authentication.service.AccessTokensStorage;
import oleg.sopilnyak.test.authentication.service.local.model.AccessCredentialsLocalEntity;
import oleg.sopilnyak.test.authentication.service.local.LocalAccessTokensStorage;
import oleg.sopilnyak.test.authentication.service.local.LocalApplicationAccessFacade;
import oleg.sopilnyak.test.authentication.service.local.LocalUserService;
import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.authentication.Role;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;

import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        LocalApplicationAccessFacade.class, LocalAccessTokensStorage.class, LocalUserService.class,
        JwtServiceImpl.class
})
class LocalApplicationAccessFacadeTest extends TestModelFactory {
    @MockitoBean
    PersistenceFacade persistenceFacade;
    @MockitoSpyBean
    @Autowired
    UserServiceAdapter userService;
    @MockitoSpyBean
    @Autowired
    AccessTokensStorage tokenStorage;

    @MockitoSpyBean
    @Autowired
    JwtServiceImpl jwtService;
    @MockitoSpyBean
    @Autowired
    ApplicationAccessFacadeAdapter facade;
    @Mock
    AccessCredentialsLocalEntity accessCredentials;
    @Mock
    UserDetailsType userDetails;

    @Test
    void checkServiceIntegrity() {
        assertThat(jwtService).isNotNull();
        assertThat(facade).isNotNull();
        assertThat(ReflectionTestUtils.getField(facade, "userService")).isSameAs(userService);
        assertThat(ReflectionTestUtils.getField(facade, "tokenStorage")).isSameAs(tokenStorage);
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
    void shouldNotFindCredentialsFor_NotSignedIn() {
        // preparing test data
        String username = "username";

        // acting
        Optional<AccessCredentials> result = facade.findCredentialsFor(username);

        // check the result
        assertThat(result).isNotNull().isEmpty();
        // check the behavior
        verify(tokenStorage).findCredentials(username);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldGrantCredentialsForUsernamePassword() throws NoSuchAlgorithmException {
        // preparing test data
        Long profileId = 1L;
        Long personId = 2L;
        String username = "username";
        String password = "password";
        FakePrincipalProfile profile = (FakePrincipalProfile) makePrincipalProfile(profileId);
        profile.setUsername(username);
        profile.setSignature(profile.makeSignatureFor(password));
        profile.setRole(Role.SUPPORT_STAFF);
        profile.setPermissions(Set.of(Permission.EDU_GET));
        doReturn(Optional.of(profile)).when(persistenceFacade).findPrincipalProfileByLogin(username);
        AuthorityPerson person = mock(AuthorityPerson.class);
        doReturn(personId).when(person).getId();
        doReturn(Optional.of(person)).when(persistenceFacade).findAuthorityPersonByProfileId(profileId);

        // acting
        Optional<AccessCredentials> result = facade.grantCredentialsFor(username, password);

        // check the result
        assertThat(result).isNotNull().isNotEmpty();
        assertThat(result.orElseThrow()).isInstanceOf(AccessCredentialsLocalEntity.class);
        AccessCredentialsLocalEntity entity = (AccessCredentialsLocalEntity) result.orElseThrow();
        assertThat(entity.getToken()).isNotNull().isNotEmpty();
        assertThat(entity.getRefreshToken()).isNotNull().isNotEmpty();
        UserDetailsType builtUserDetails = entity.getUser();
        assertThat(builtUserDetails.getUsername()).isEqualTo(username);
        assertThat(builtUserDetails.getPassword()).isEqualTo(password);
        Collection<GrantedAuthority> grantedAuthorities = List.copyOf(builtUserDetails.getAuthorities());
        assertThat(grantedAuthorities).contains(new SimpleGrantedAuthority("EDU_GET"), new SimpleGrantedAuthority("ROLE_SUPPORT_STAFF"));
        // check the behavior
        verify(userService).prepareUserDetails(username, password);
        verify(persistenceFacade).findPrincipalProfileByLogin(username);
        verify(persistenceFacade).findAuthorityPersonByProfileId(profileId);
        ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
        verify(userService).toModel(eq(personId), eq(username), eq(password), captor.capture());
        assertThat(captor.getValue()).containsAll(builtUserDetails.getAuthorities());
        verify(facade).grantCredentialsFor(builtUserDetails);
        verify(facade).buildFor(builtUserDetails);
    }

    @Test
    void shouldNotGrantCredentialsForUsernamePassword_WrongPassword() {
        // preparing test data
        Long profileId = 3L;
        String username = "username";
        String password = "password";
        FakePrincipalProfile profile = (FakePrincipalProfile) makePrincipalProfile(profileId);
        profile.setUsername(username);
        profile.setRole(Role.SUPPORT_STAFF);
        profile.setPermissions(Set.of(Permission.EDU_GET));
        doReturn(Optional.of(profile)).when(persistenceFacade).findPrincipalProfileByLogin(username);

        // acting
        Exception error = assertThrows(Exception.class, () -> facade.grantCredentialsFor(username, password));

        // check the result
        assertThat(error).isInstanceOf(SchoolAccessDeniedException.class);
        assertThat(error.getMessage()).isEqualTo("Wrong password for username: " + username);
        // check the behavior
        verify(persistenceFacade).findPrincipalProfileByLogin(username);
        verify(facade, never()).grantCredentialsFor(any(UserDetailsType.class));
    }

    @Test
    void shouldNotGrantCredentialsForUsernamePassword_NoPersonForProfile() throws NoSuchAlgorithmException {
        // preparing test data
        Long profileId = 4L;
        String username = "username";
        String password = "password";
        FakePrincipalProfile profile = (FakePrincipalProfile) makePrincipalProfile(profileId);
        profile.setUsername(username);
        profile.setSignature(profile.makeSignatureFor(password));
        profile.setRole(Role.SUPPORT_STAFF);
        profile.setPermissions(Set.of(Permission.EDU_GET));
        doReturn(Optional.of(profile)).when(persistenceFacade).findPrincipalProfileByLogin(username);

        // acting
        Exception error = assertThrows(Exception.class, () -> facade.grantCredentialsFor(username, password));

        // check the result
        assertThat(error).isInstanceOf(UsernameNotFoundException.class);
        assertThat(error.getMessage()).isEqualTo("Person with username: '" + username + "' isn't found!");
        // check the behavior
        verify(userService).prepareUserDetails(username, password);
        verify(persistenceFacade).findPrincipalProfileByLogin(username);
        verify(persistenceFacade).findAuthorityPersonByProfileId(profileId);
        verify(facade, never()).grantCredentialsFor(any(UserDetailsType.class));
    }

    @Test
    void shouldGrantCredentialsForUserDetails() {
        // preparing test data
        String activeToken = "activeToken";
        String refreshToken = "refreshToken";
        doReturn(activeToken).when(jwtService).generateAccessToken(userDetails);
        doReturn(refreshToken).when(jwtService).generateRefreshToken(userDetails);

        // acting
        Optional<AccessCredentials> result = facade.grantCredentialsFor(userDetails);

        // check the result
        assertThat(result).isNotNull().isNotEmpty();
        assertThat(result.orElseThrow()).isInstanceOf(AccessCredentialsType.class);
        AccessCredentialsType entity = (AccessCredentialsType) result.orElseThrow();
        assertThat(entity.getToken()).isSameAs(activeToken);
        assertThat(entity.getRefreshToken()).isSameAs(refreshToken);
        assertThat(entity.getUser()).isSameAs(userDetails);
        // check the behavior
        verify(jwtService).generateAccessToken(userDetails);
        verify(jwtService).generateRefreshToken(userDetails);
        verify(facade).buildFor(userDetails);
    }

    @Test
    void shouldRefreshCredentialsFor() {
        // preparing test data
        Long profileId = 5L;
        String username = "username";
        String refreshToken = "refresh-token";
        // preparing tokens storage
        doReturn(false).when(jwtService).isTokenExpired(refreshToken);
        doReturn(username).when(jwtService).extractUserName(refreshToken);
        doReturn(Optional.of(accessCredentials)).when(tokenStorage).findCredentials(username);
        doReturn(userDetails).when(accessCredentials).getUser();
        // preparing person's profile
        FakePrincipalProfile profile = (FakePrincipalProfile) makePrincipalProfile(profileId);
        profile.setUsername(username);
        profile.setRole(Role.SUPPORT_STAFF);
        profile.setPermissions(Set.of(Permission.EDU_GET));
        // preparing user-details
        doReturn(username).when(userDetails).getUsername();
        doReturn("password").when(userDetails).getPassword();
        Collection<GrantedAuthority> authorities = authorities(profile);
        doReturn(authorities).when(userDetails).getAuthorities();

        // acting
        Optional<AccessCredentials> result = facade.refreshCredentialsFor(username, refreshToken);

        // check the result
        assertThat(result).isNotNull().isNotEmpty();
        AccessCredentialsType entity = (AccessCredentialsType) result.orElseThrow();
        assertThat(entity).isInstanceOf(AccessCredentialsLocalEntity.class);
        assertThat(entity.getUser()).isSameAs(userDetails);
        assertThat(entity.getToken()).isNotNull().isNotEmpty();
        assertThat(entity.getRefreshToken()).isNotNull().isNotEmpty().isNotEqualTo(refreshToken);
        // check the behavior
        verify(jwtService).isTokenExpired(refreshToken);
        verify(jwtService).extractUserName(refreshToken);
        verify(tokenStorage).findCredentials(username);
        verify(facade).grantCredentialsFor(userDetails);
        verify(facade).buildFor(userDetails);
        verify(jwtService).generateAccessToken(userDetails);
        verify(jwtService).generateRefreshToken(userDetails);
    }

    @Test
    void shouldNotRefreshCredentialsFor_RefreshTokenIsExpired() {
        // preparing test data
        String username = "username";
        String refreshToken = "refresh-token";
        // preparing tokens storage
        doReturn(true).when(jwtService).isTokenExpired(refreshToken);

        // acting
        Optional<AccessCredentials> result = facade.refreshCredentialsFor(username, refreshToken);

        // check the result
        assertThat(result).isNotNull().isEmpty();
        // check the behavior
        verify(jwtService).isTokenExpired(refreshToken);
        verify(jwtService, never()).extractUserName(anyString());
        verify(tokenStorage).deleteCredentialsWithRefreshToken(refreshToken);
    }

    @Test
    void shouldNotRefreshCredentialsFor_NotSignedIn() {
        // preparing test data
        String username = "username";
        String refreshToken = "refresh-token";
        // preparing tokens storage
        doReturn(false).when(jwtService).isTokenExpired(refreshToken);
        doReturn(username).when(jwtService).extractUserName(refreshToken);

        // acting
        Exception error = assertThrows(RuntimeException.class, () -> facade.refreshCredentialsFor(username, refreshToken));

        // check the result
        assertThat(error).isInstanceOf(SchoolAccessDeniedException.class);
        assertThat(error.getMessage()).isEqualTo("Person with username: '" + username + "' isn't signed in");
        // check the behavior
        verify(jwtService).isTokenExpired(refreshToken);
        verify(jwtService).extractUserName(refreshToken);
        verify(tokenStorage).findCredentials(username);
        verify(facade, never()).grantCredentialsFor(any(UserDetailsType.class));
    }

    @Test
    void shouldNotRefreshCredentialsFor_WrongUserDetails() {
        // preparing test data
        String username = "username";
        String refreshToken = "refresh-token";
        // preparing tokens storage
        doReturn(false).when(jwtService).isTokenExpired(refreshToken);
        doReturn(username).when(jwtService).extractUserName(refreshToken);
        doReturn(Optional.of(accessCredentials)).when(tokenStorage).findCredentials(username);
        doReturn(userDetails).when(accessCredentials).getUser();
        // preparing user-details
        doReturn(username).when(userDetails).getUsername();
        doReturn("password").when(userDetails).getPassword();

        // acting
        Exception error = assertThrows(RuntimeException.class, () -> facade.refreshCredentialsFor(username, refreshToken));

        // check the result
        assertThat(error).isInstanceOf(IllegalArgumentException.class);
        assertThat(error.getMessage()).isEqualTo("UserDetails, no roles declared!");
        // check the behavior
        verify(jwtService).isTokenExpired(refreshToken);
        verify(jwtService).extractUserName(refreshToken);
        verify(tokenStorage).findCredentials(username);
        verify(facade).grantCredentialsFor(userDetails);
        verify(facade).buildFor(userDetails);
        verify(jwtService).generateAccessToken(userDetails);
        verify(jwtService, never()).generateRefreshToken(any(UserDetails.class));
    }

    // private methods
    private Collection<GrantedAuthority> authorities(PrincipalProfile profile) {
        final Set<GrantedAuthority> authorities = new HashSet<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + profile.getRole().name()));
        profile.getPermissions().stream().map(Enum::name)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);
        return authorities;
    }

}