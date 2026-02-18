package oleg.sopilnyak.test.authentication.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtServiceImplTest {
    JwtServiceImpl service;
    UserDetails userDetails;

    String username = "username";

    @BeforeEach
    void setUp() {
        userDetails = mock(UserDetails.class);
        service = spy(new JwtServiceImpl());
        service.initSecurityKey();
        reset(service);
    }

    @Test
    void shouldInitSecurityKey() {
        ReflectionTestUtils.setField(service, "signingKey", null);
        assertThat(ReflectionTestUtils.getField(service, "signingKey")).isNull();

        service.initSecurityKey();

        assertThat(ReflectionTestUtils.getField(service, "signingKey")).isNotNull();
    }

    @Test
    void shouldExtractUserName() {
        doReturn(username).when(userDetails).getUsername();
        String token = service.generateAccessToken(Map.of(), userDetails);
        reset(service);

        String value = service.extractUserName(token);

        assertThat(value).isEqualTo(username);
    }

    @Test
    void shouldNotExtractUserName_NullUsername() {
        String token = service.generateAccessToken(Map.of(), userDetails);
        reset(service);

        String value = service.extractUserName(token);

        assertThat(value).isNotEqualTo(username).isNull();
    }

    @Test
    void shouldNotExtractUserName_ExpiredToken() {
        doReturn(username).when(userDetails).getUsername();
        JwtBuilder builder = ReflectionTestUtils.invokeMethod(service, "builderForUser", userDetails);
        assertThat(builder).isNotNull();
        Date expiredDate = Date.from(Instant.now().minus(1, TimeUnit.MILLISECONDS.toChronoUnit()));
        builder.expiration(expiredDate);
        String token = builder.compact();

        String value = service.extractUserName(token);

        assertThat(value).isNotEqualTo(username).isNull();
    }

    @Test
    void shouldNotExtractUserName_WrongToken() {
        String token = "service.generateAccessToken(Map.of(), userDetails)";

        String value = service.extractUserName(token);

        assertThat(value).isNotEqualTo(username).isNull();
    }

    @Test
    void shouldBeTokenIsValid() {
        doReturn(username).when(userDetails).getUsername();
        String token = service.generateAccessToken(Map.of(), userDetails);
        reset(service);

        boolean valid = service.isTokenValid(token, userDetails);

        assertThat(valid).isTrue();
    }

    @Test
    void shouldBeTokenNotValid_WrongToken() {
        doReturn(username).when(userDetails).getUsername();
        String token = "service.generateAccessToken(Map.of(), userDetails)";

        boolean valid = service.isTokenValid(token, userDetails);

        assertThat(valid).isFalse();
    }

    @Test
    void shouldBeTokenNotExpired() {
        doReturn(username).when(userDetails).getUsername();
        String token = service.generateAccessToken(Map.of(), userDetails);
        reset(service);

        boolean expired = service.isTokenExpired(token);

        assertThat(expired).isFalse();
    }

    @Test
    void shouldBeTokenIsExpired() {
        JwtBuilder builder = ReflectionTestUtils.invokeMethod(service, "builderForUser", userDetails);
        Date expiredDate = Date.from(Instant.now().minus(1, TimeUnit.MILLISECONDS.toChronoUnit()));
        assertThat(builder).isNotNull();
        builder.expiration(expiredDate);
        String token = builder.compact();

        boolean expired = service.isTokenExpired(token);

        assertThat(expired).isTrue();
    }

    @Test
    void shouldGenerateAccessToken_Standard() {
        doReturn(username).when(userDetails).getUsername();

        String token = service.generateAccessToken(Map.of(), userDetails);

        assertThat(token).isNotNull().isNotBlank().isNotEmpty();
        Claims claims = ReflectionTestUtils.invokeMethod(service, "extractAllClaims", token);
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(username);
        assertThat(claims.getExpiration()).isAfter(new Date());
    }

    @Test
    void shouldGenerateAccessToken_Extended() {
        doReturn(username).when(userDetails).getUsername();

        String token = service.generateAccessToken(Map.of("id", 1, "description", "It's cool"), userDetails);

        assertThat(token).isNotNull().isNotBlank().isNotEmpty();
        Claims claims = ReflectionTestUtils.invokeMethod(service, "extractAllClaims", token);
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(username);
        assertThat(claims.getExpiration()).isAfter(new Date());
        assertThat(claims.get("id")).isEqualTo(1);
        assertThat(claims.get("description")).isEqualTo("It's cool");
    }

    @Test
    void shouldGenerateRefreshToken() {
        doReturn(username).when(userDetails).getUsername();

        String token = service.generateRefreshToken(userDetails);

        assertThat(token).isNotNull().isNotBlank().isNotEmpty();
        Claims claims = ReflectionTestUtils.invokeMethod(service, "extractAllClaims", token);
        assertThat(claims).isNotNull();
        assertThat(claims.getSubject()).isEqualTo(username);
        assertThat(claims.getExpiration()).isNull();
    }
}