package oleg.sopilnyak.test.authentication.service.infinispan;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.authentication.service.AccessTokensStorage;
import oleg.sopilnyak.test.authentication.service.JwtService;
import oleg.sopilnyak.test.authentication.service.infinispan.model.AccessCredentialsProto;
import oleg.sopilnyak.test.authentication.service.infinispan.model.ProhibitedTokensProto;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;

import jakarta.annotation.PostConstruct;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.infinispan.Cache;
import org.infinispan.commons.api.CacheContainerAdmin;
import org.infinispan.commons.dataconversion.MediaType;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.springframework.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Storage: Implementation (through Infinispan) of the storage of active tokens
 */
@Slf4j
@RequiredArgsConstructor
public class DistributeAccessTokensStorage implements AccessTokensStorage {
    private static final String BLACK_LIST_KEY = "black-list-tokens";
    private static final String ACCESS_CREDENTIALS_CACHE = "accessCredentialsCache";
    private static final String BLACK_LIST_TOKENS_CACHE = "blackListTokensCache";
    // services used in the access credentials storage
    private final DefaultCacheManager cacheManager;
    private final JwtService jwtService;
    private Cache<String, AccessCredentials> accessCredentials;
    private Cache<String, ProhibitedTokensProto> blackList;

    @PostConstruct
    public void buildCaches() {
        final ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.clustering().cacheMode(CacheMode.DIST_SYNC)
                .encoding().key().mediaType(MediaType.TEXT_PLAIN_TYPE)
//                .encoding().key().mediaType(MediaType.APPLICATION_PROTOSTREAM_TYPE)
                .encoding().value().mediaType(MediaType.APPLICATION_PROTOSTREAM_TYPE);
        // prepare caches for infinispan
        accessCredentials = cacheManager.administration()
                .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
                .getOrCreateCache(ACCESS_CREDENTIALS_CACHE, builder.build());
        blackList = cacheManager.administration()
                .withFlags(CacheContainerAdmin.AdminFlag.VOLATILE)
                .getOrCreateCache(BLACK_LIST_TOKENS_CACHE, builder.build());
        //
        // store empty black-list if it's not exists there
        blackList.putIfAbsent(BLACK_LIST_KEY, ProhibitedTokensProto.of(List.of()));
    }

    /**
     * Storing signed in credentials for further usage
     *
     * @param username    person's access username
     * @param credentials access credentials
     * @see AccessCredentials
     */
    @Override
    public void storeFor(final String username, final AccessCredentials credentials) {
        log.debug("Storing access credentials for {}", username);
        if (credentials instanceof AccessCredentialsProto protoCredentials) {
            accessCredentials.put(username, protoCredentials);
            log.debug("Stored access credentials for {}", username);
        } else {
            log.warn("Unsupported access credentials for '{}' wrong one is: {}", username, credentials);
            throw new ClassCastException("Credentials is not of type AccessCredentialsProto");
        }
    }

    /**
     * Deleting stored person's credentials
     *
     * @param username person's access username
     */
    @Override
    public void deleteCredentials(final String username) {
        log.debug("Deleting access credentials for {}", username);
        accessCredentials.remove(username);
    }

    /**
     * Deleting stored person's credentials
     *
     * @param refreshToken person's access refresh-token
     */
    @Override
    public void deleteCredentialsWithRefreshToken(final String refreshToken) {
        accessCredentials.values().stream()
                .filter(credentials -> credentials.getRefreshToken().equals(refreshToken))
                .map(AccessCredentialsProto.class::cast)
                .map(credentials -> credentials.getUser().getUsername())
                .findFirst().ifPresent(this::deleteCredentials);
    }

    /**
     * To find stored credentials
     *
     * @param username person's access username
     * @return access credentials or empty
     * @see Optional
     * @see AccessCredentials
     */
    @Override
    public Optional<AccessCredentials> findCredentials(final String username) {
        log.debug("Finding access credentials for {}", username);
        return isNull(username) ? Optional.empty() : Optional.ofNullable(accessCredentials.get(username));
    }

    /**
     * To add token to black list for further token's ignoring
     *
     * @param token token to ignore
     */
    @Override
    public void toBlackList(final String token) {
        log.debug("Putting to black list token: '{}'", token);
        if (!StringUtils.hasText(token)) {
            log.warn("=== token '{}' is empty!", token);
            return;
        }
        final var blackListedTokens = blackList.computeIfAbsent(BLACK_LIST_KEY, _ -> ProhibitedTokensProto.of(List.of()));
        final Collection<String> prohibited = Stream.concat(
                        blackListedTokens.getProhibitedTokens().stream(), Stream.of(token)
                )
                .collect(Collectors.toCollection(LinkedHashSet::new)).stream().toList();
        //
        // store updates to the cache
        blackList.replace(BLACK_LIST_KEY, ProhibitedTokensProto.of(prohibited));
    }

    /**
     * To remove token from black list
     *
     * @param token token to remove from black-list
     */
    @Override
    public void removeFromBlackList(final String token) {
        log.debug("Removing token '{}' from black list", token);
        final var blackListedTokens = blackList.computeIfAbsent(BLACK_LIST_KEY, _ -> ProhibitedTokensProto.of(List.of()));
        final Collection<String> prohibited = blackListedTokens.getProhibitedTokens().stream()
                .filter(value -> !Objects.equals(value, token)).toList();
        //
        // store updates to the cache
        blackList.replace(BLACK_LIST_KEY, ProhibitedTokensProto.of(prohibited));
    }

    /**
     * To check is the token of signed-out person
     *
     * @param token active token of signed-out person
     * @return true if token is black-listed
     */
    @Override
    public boolean isInBlackList(final String token) {
        log.debug("Checking token: '{}' in black list", token);
        if (jwtService.isTokenExpired(token)) {
            log.warn("Detected expired token: '{}'", token);
            removeFromBlackList(token);
            return false;
        } else {
            log.debug("Checking black-list for token: '{}'", token);
            return blackList.get(BLACK_LIST_KEY).hasToken(token);
        }
    }
}
