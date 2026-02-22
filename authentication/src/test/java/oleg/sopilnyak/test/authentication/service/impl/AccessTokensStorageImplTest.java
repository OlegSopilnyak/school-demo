package oleg.sopilnyak.test.authentication.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.authentication.service.AccessTokensStorage;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AccessTokensStorageImplTest {

    JwtServiceImpl jwtService;
    AccessTokensStorage storage;

    final String username = "username";
    final String accessToken = "access-token";
    @Mock
    AccessCredentials credentials;
    Map<String, AccessCredentials> accessCredentials;
    Set<String> blackList;

    @BeforeEach
    void setUp() {
        jwtService = spy(new JwtServiceImpl());
        storage = spy(new AccessTokensStorageImpl(jwtService));
        accessCredentials = spy(new ConcurrentHashMap<>());
        blackList = ConcurrentHashMap.newKeySet();
        ReflectionTestUtils.setField(storage, "accessCredentials", accessCredentials);
        ReflectionTestUtils.setField(storage, "blackList", blackList);
    }

    @Test
    void shouldStoreFor() {
        assertThat(accessCredentials).isEmpty();

        storage.storeFor(username, credentials);

        assertThat(accessCredentials).hasSize(1).containsEntry(username, credentials);
        verify(accessCredentials).put(username, credentials);
    }

    @Test
    void shouldDeleteCredentials() {
        assertThat(accessCredentials).isEmpty();
        accessCredentials.put(username, credentials);

        storage.deleteCredentials(username);

        assertThat(accessCredentials).isEmpty();
        verify(accessCredentials).remove(username);
    }

    @Test
    void shouldFindCredentials() {
        assertThat(accessCredentials).isEmpty();
        accessCredentials.put(username, credentials);

        var creds = storage.findCredentials(username);

        assertThat(creds).isPresent().contains(credentials);
        verify(accessCredentials).get(username);
    }

    @Test
    void shouldNotFindCredentials() {
        assertThat(accessCredentials).isEmpty();

        var creds = storage.findCredentials(username);

        assertThat(creds).isEmpty();
        verify(accessCredentials).get(username);
    }

    @Test
    void shouldPutToBlackList() {
        assertThat(blackList).isEmpty();

        storage.toBlackList(accessToken);

        assertThat(blackList).contains(accessToken);
    }

    @Test
    void shouldBeInBlackList_TokenIsNotExpired() {
        assertThat(blackList).isEmpty();
        blackList.add(accessToken);
        doReturn(false).when(jwtService).isTokenExpired(accessToken);

        var result = storage.isInBlackList(accessToken);

        assertThat(result).isTrue();
        assertThat(blackList).contains(accessToken);
    }

    @Test
    void shouldBeInBlackList_TokenIsExpired() {
        assertThat(blackList).isEmpty();
        blackList.add(accessToken);
        doReturn(true).when(jwtService).isTokenExpired(accessToken);

        var result = storage.isInBlackList(accessToken);

        assertThat(result).isFalse();
        assertThat(blackList).isEmpty();
    }

    @Test
    void shouldNotBeInBlackList_WrongToken() {
        assertThat(blackList).isEmpty();
        blackList.add(accessToken);
        String token = "wrong-token";
        doReturn(false).when(jwtService).isTokenExpired(token);

        var result = storage.isInBlackList(token);

        assertThat(result).isFalse();
        assertThat(blackList).contains(accessToken);
    }
}