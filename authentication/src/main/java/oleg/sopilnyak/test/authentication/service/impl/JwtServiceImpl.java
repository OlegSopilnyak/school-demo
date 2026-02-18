package oleg.sopilnyak.test.authentication.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import oleg.sopilnyak.test.authentication.service.JwtService;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JwtServiceImpl implements JwtService {
    // secret key to sign and verify the token
    private SecretKey signingKey;

    @PostConstruct
    public void initSecurityKey() {
        final String jwtSigningKey = Base64.getEncoder().encodeToString(TRUTH.getBytes(StandardCharsets.UTF_8));
        final byte[] keyBytes = Decoders.BASE64.decode(jwtSigningKey);
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * To extract username from the JW Token
     *
     * @param token jwt token
     * @return username from the token
     * @see StringUtils#hasText(String)
     * @see Claims#getSubject()
     */
    @Override
    public String extractUserName(final String token) {
        return StringUtils.hasText(token) ? extractClaim(token, Claims::getSubject) : null;
    }


    /**
     * To check the token, is it complain to user-details
     *
     * @param token       jwt to check
     * @param userDetails to check username with one from the token
     * @return true if token is valid
     * @see UserDetails#getUsername()
     * @see JwtService#extractUserName(String)
     */
    @Override
    public boolean isTokenValid(final String token, final UserDetails userDetails) {
        return Objects.equals(extractUserName(token), userDetails.getUsername());
    }

    /**
     * To check is access token expired
     *
     * @param token jwt to check
     * @return true if token is expired
     */
    @Override
    public boolean isTokenExpired(final String token) {
        return extractExpiration(token).before(nowDate());
    }

    /**
     * To generate access token
     *
     * @param extraClaims the claims of the token
     * @param userDetails user-details of the token
     * @return generated token's instance
     */
    @Override
    public String generateAccessToken(final Map<String, Object> extraClaims, final UserDetails userDetails) {
        final JwtBuilder builder = builderForUser(userDetails);
        return builder.expiration(nowDatePlus(24, TimeUnit.MINUTES)).claims(extraClaims).compact();
    }

    /**
     * To generate access refresh token
     *
     * @param userDetails user-details of the token
     * @return generated token's instance
     */
    @Override
    public String generateRefreshToken(final UserDetails userDetails) {
        return builderForUser(userDetails).compact();
    }

    // private methods
    private JwtBuilder builderForUser(final UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuer(ISSUER)
                .issuedAt(nowDate())
                .signWith(signingKey)
                ;
    }

    private static Date nowDate() {
        final Instant now = Instant.now();
        return Date.from(now);
    }

    private static Date nowDatePlus(final long duration, final TimeUnit timeUnit) {
        final Instant now = Instant.now();
        return Date.from(now.plus(duration, timeUnit.toChronoUnit()));
    }

    private <T> T extractClaim(final String token, final Function<Claims, T> claimsResolvers) {
        try {
            return claimsResolvers.apply(extractAllClaims(token));
        } catch (Exception e) {
            log.error("Cannot parse Claims of token: {}", token, e);
            return null;
        }
    }

    private Date extractExpiration(final String token) {
        final Date expirationDate = extractClaim(token, Claims::getExpiration);
        return expirationDate == null ? new Date(0L) : expirationDate;
    }

    private Claims extractAllClaims(final String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }
}
