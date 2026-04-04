package oleg.sopilnyak.test.authentication.service.infinispan;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.authentication.configuration.JwtConfiguration;
import oleg.sopilnyak.test.authentication.model.AccessCredentialsType;
import oleg.sopilnyak.test.authentication.service.AccessTokensStorage;
import oleg.sopilnyak.test.authentication.service.JwtService;
import oleg.sopilnyak.test.authentication.service.infinispan.model.AccessCredentialsProto;
import oleg.sopilnyak.test.authentication.service.infinispan.model.ProhibitedTokensProto;
import oleg.sopilnyak.test.authentication.service.infinispan.model.UserDetailsProto;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.school.common.test.TestModelFactory;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {JwtConfiguration.class})
@TestPropertySource(properties = "application.infinispan.cluster.name=temporary-infinispan-cluster")
@ActiveProfiles("distribute")
@SuppressWarnings("unchecked")
class DistributeAccessTokensStorageTest extends TestModelFactory {
    private static final String KEY = "black-list-tokens";
    @MockitoBean
    PersistenceFacade persistenceFacade;
    @Autowired
    @MockitoSpyBean
    JwtService jwtService;
    @Autowired
    @MockitoSpyBean
    AccessTokensStorage storage;
    @Autowired
    @MockitoSpyBean
    DefaultCacheManager cacheManager;
    Cache<String, AccessCredentials> accessCredentials;
    Cache<String, ProhibitedTokensProto> blackList;

    @BeforeEach
    void setUp() {
        accessCredentials = (Cache<String, AccessCredentials>) ReflectionTestUtils.getField(storage, "accessCredentials");
        blackList = (Cache<String, ProhibitedTokensProto>) ReflectionTestUtils.getField(storage, "blackList");
        assertThat(blackList).isNotNull();
        blackList.putIfAbsent(KEY, ProhibitedTokensProto.of(List.of()));
    }

    @AfterEach
    void tearDown() {
        accessCredentials.clear();
        blackList.clear();
    }

    @Test
    void checkTestIntegrity() {
        assertThat(cacheManager).isNotNull();
        assertThat(storage).isInstanceOf(DistributeAccessTokensStorage.class);
    }

    @Test
    void shouldBuildCaches() {
        // preparing test data
        Long personId = 1L;
        AccessCredentialsType credentials = createAccessCredentialsFor(personId);
        UserDetailsProto user = (UserDetailsProto) credentials.getUser();
        String username = user.getUsername();
        assertThat(storage.findCredentials(username)).isEmpty();
        storage.storeFor(username, credentials);

        // acting
        ((DistributeAccessTokensStorage) storage).buildCaches();

        // check the result
        assertThat(accessCredentials).isSameAs(ReflectionTestUtils.getField(storage, "accessCredentials"));
        AccessCredentials result = accessCredentials.get(username);
        assertThat(result).isInstanceOf(AccessCredentialsProto.class).isNotSameAs(credentials).isEqualTo(credentials);
        assertThat(result.getId()).isNull();
        assertThat(result.getToken()).isEqualTo(credentials.getToken());
        assertThat(result.getRefreshToken()).isEqualTo(credentials.getRefreshToken());
        UserDetailsProto credentialsUser = (UserDetailsProto) ReflectionTestUtils.getField(result, "user");
        assertThat(credentialsUser).isNotSameAs(user).isEqualTo(user);
        assertThat(credentialsUser.getId()).isEqualTo(personId);
        assertThat(credentialsUser.getUsername()).isEqualTo(username);
        assertThat(credentialsUser.getPassword()).isEqualTo(user.getPassword());
        assertThat(credentialsUser.getAuthorityNames()).containsAll(user.getAuthorityNames());
        assertThat(credentialsUser.getAuthorities()).hasSameSizeAs(user.getAuthorities());
        // check the behavior
    }

    @Test
    void shouldStoreAccessCredentials() {
        // preparing test data
        Long personId = 2L;
        AccessCredentialsType credentials = createAccessCredentialsFor(personId);
        UserDetailsProto user = (UserDetailsProto) credentials.getUser();
        String username = user.getUsername();
        assertThat(storage.findCredentials(username)).isEmpty();

        // acting
        storage.storeFor(username, credentials);

        // check the result
        assertThat(storage.findCredentials(username)).isPresent();
        AccessCredentials result = accessCredentials.get(username);
        assertThat(result).isInstanceOf(AccessCredentialsProto.class).isNotSameAs(credentials).isEqualTo(credentials);
        assertThat(result.getId()).isNull();
        assertThat(result.getToken()).isEqualTo(credentials.getToken());
        assertThat(result.getRefreshToken()).isEqualTo(credentials.getRefreshToken());
        UserDetailsProto credentialsUser = (UserDetailsProto) ReflectionTestUtils.getField(result, "user");
        assertThat(credentialsUser).isNotSameAs(user).isEqualTo(user);
        assertThat(credentialsUser.getId()).isEqualTo(personId);
        assertThat(credentialsUser.getUsername()).isEqualTo(username);
        assertThat(credentialsUser.getPassword()).isEqualTo(user.getPassword());
        assertThat(credentialsUser.getAuthorityNames()).containsAll(user.getAuthorityNames());
        assertThat(credentialsUser.getAuthorities()).hasSameSizeAs(user.getAuthorities());
        // check the behavior
    }

    @Test
    void shouldNotStoreAccessCredentials_WrongAccessCredentialsType() {
        // preparing test data
        String username = "username";
        AccessCredentialsType credentials = mock(AccessCredentialsType.class);

        // acting
        Exception error = assertThrows(RuntimeException.class, () -> storage.storeFor(username, credentials));

        // check the result
        assertThat(error).isInstanceOf(ClassCastException.class);
        assertThat(error.getMessage()).contains("AccessCredentialsProto");
        // check the behavior
    }

    @Test
    void shouldDeleteCredentials() {
        // preparing test data
        long personId = 3L;
        AccessCredentialsType credentials = createAccessCredentialsFor(personId);
        String username = credentials.getUser().getUsername();
        assertThat(storage.findCredentials(username)).isEmpty();
        storage.storeFor(username, credentials);
        AccessCredentialsType credentials2 = createAccessCredentialsFor(personId + 1);
        String secondUsername = credentials2.getUser().getUsername();
        storage.storeFor(secondUsername, credentials2);
        assertThat(storage.findCredentials(username)).isPresent();
        assertThat(storage.findCredentials(secondUsername)).isPresent();

        // acting
        storage.deleteCredentials(username);

        // check the result
        assertThat(storage.findCredentials(username)).isEmpty();
        assertThat(storage.findCredentials(secondUsername)).isPresent();
        // check the behavior
    }

    @Test
    void shouldDeleteCredentialsWithRefreshToken() {
        // preparing test data
        long personId = 5L;
        AccessCredentialsType credentials = createAccessCredentialsFor(personId);
        String username = credentials.getUser().getUsername();
        String refreshToken = credentials.getRefreshToken();
        assertThat(storage.findCredentials(username)).isEmpty();
        storage.storeFor(username, credentials);
        AccessCredentialsType credentials2 = createAccessCredentialsFor(personId + 1);
        String secondUsername = credentials2.getUser().getUsername();
        storage.storeFor(secondUsername, credentials2);
        assertThat(storage.findCredentials(username)).isPresent();
        assertThat(storage.findCredentials(secondUsername)).isPresent();

        // acting
        storage.deleteCredentialsWithRefreshToken(refreshToken);

        // check the result
        assertThat(storage.findCredentials(username)).isEmpty();
        assertThat(storage.findCredentials(secondUsername)).isPresent();
        // check the behavior
        verify(storage).deleteCredentials(username);
    }

    @Test
    void shouldFindCredentials() {
        // preparing test data
        long personId = 7L;
        AccessCredentialsType credentials = createAccessCredentialsFor(personId);
        UserDetailsProto user = (UserDetailsProto) credentials.getUser();
        String username = user.getUsername();

        // acting
        assertThat(storage.findCredentials(username)).isEmpty();
        storage.storeFor(username, credentials);
        assertThat(storage.findCredentials(username)).isPresent();

        // check the result
        AccessCredentials result = accessCredentials.get(username);
        assertThat(result).isInstanceOf(AccessCredentialsProto.class).isNotSameAs(credentials).isEqualTo(credentials);
        assertThat(result.getId()).isNull();
        assertThat(result.getToken()).isEqualTo(credentials.getToken());
        assertThat(result.getRefreshToken()).isEqualTo(credentials.getRefreshToken());
        UserDetailsProto credentialsUser = (UserDetailsProto) ReflectionTestUtils.getField(result, "user");
        assertThat(credentialsUser).isNotSameAs(user).isEqualTo(user);
        assertThat(credentialsUser.getId()).isEqualTo(personId);
        assertThat(credentialsUser.getUsername()).isEqualTo(username);
        assertThat(credentialsUser.getPassword()).isEqualTo(user.getPassword());
        assertThat(credentialsUser.getAuthorityNames()).containsAll(user.getAuthorityNames());
        assertThat(credentialsUser.getAuthorities()).hasSameSizeAs(user.getAuthorities());
        // check the behavior
    }

    @Test
    void shouldPutToBlackList() {
        // preparing test data
        String blackListToken = UUID.randomUUID().toString();
        assertThat(blackList.get(KEY).isEmpty()).isTrue();

        // acting
        storage.toBlackList(blackListToken);

        // check the result
        assertThat(blackList.get(KEY).isEmpty()).isFalse();
        assertThat(blackList.get(KEY).hasToken(blackListToken)).isTrue();
        // check the behavior
    }

    @Test
    void shouldNotPutToBlackList_EmptyToken() {
        // preparing test data
        String blackListToken = "   ";
        assertThat(blackList.get(KEY).isEmpty()).isTrue();

        // acting
        storage.toBlackList(blackListToken);

        // check the result
        assertThat(blackList.get(KEY).isEmpty()).isTrue();
        // check the behavior
    }

    @Test
    void shouldRemoveFromBlackList() {
        // preparing test data
        String blackListToken = UUID.randomUUID().toString();
        blackList.put(KEY, ProhibitedTokensProto.of(Set.of(blackListToken)));

        // acting
        storage.removeFromBlackList(blackListToken);

        // check the result
        assertThat(blackList.get(KEY).isEmpty()).isTrue();
        assertThat(blackList.get(KEY).hasToken(blackListToken)).isFalse();
        // check the behavior
    }

    @Test
    void shouldBeInBlackList() {
        // preparing test data
        String blackListToken = UUID.randomUUID().toString();
        blackList.put(KEY, ProhibitedTokensProto.of(Set.of(blackListToken)));
        doReturn(false).when(jwtService).isTokenExpired(blackListToken);

        // acting
        boolean result = storage.isInBlackList(blackListToken);

        // check the result
        assertThat(result).isTrue();
        // check the behavior
        verify(storage, never()).removeFromBlackList(anyString());
    }

    @Test
    void shouldNotBeInBlackList_TokenIsExpired() {
        // preparing test data
        String blackListToken = UUID.randomUUID().toString();
        blackList.put(KEY, ProhibitedTokensProto.of(Set.of(blackListToken)));
        doReturn(true).when(jwtService).isTokenExpired(blackListToken);

        // acting
        boolean result = storage.isInBlackList(blackListToken);

        // check the result
        assertThat(result).isFalse();
        // check the behavior
        verify(storage).removeFromBlackList(blackListToken);
    }

    // private methods
    private static AccessCredentialsType createAccessCredentialsFor(Long personId) {
        String username = "username-" + personId;
        String password = "password";
        String token = "token-" + personId;
        String refreshToken = "refresh-token-" + personId;
        List<String> authorities = List.of("ROLE_USER", "GET_DATA_" + personId);
        UserDetailsProto user = UserDetailsProto.builder()
                .id(personId).username(username).password(password).authorityNames(authorities)
                .build();
        return AccessCredentialsProto.builder()
                .token(token).refreshToken(refreshToken).user(user)
                .build();
    }
}
