package oleg.sopilnyak.test.authentication.service.impl;

import io.jsonwebtoken.Claims;
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
     * @see Claims#getSubject()
     */
    @Override
    public String extractUserName(final String token) {
        return token == null || token.isBlank() ? null : extractClaim(token, Claims::getSubject);
    }


    /**
     * To check the token, is it complain to user-details
     *
     * @param token       jwt to check
     * @param userDetails to check username with one from the token
     * @return true if token is valid
     * @see UserDetails#getUsername()
     * @see JwtService#extractUserName(String)
     * @see JwtService#isTokenExpired(String)
     */
    @Override
    public boolean isTokenValid(final String token, final UserDetails userDetails) {
        final String userName = extractUserName(token);
        return Objects.equals(userName, userDetails.getUsername()) && !isTokenExpired(token);
    }

    @Override
    public boolean isTokenExpired(final String token) {
        return extractExpiration(token).before(nowDate());
    }

    @Override
    public String generateAccessToken(final Map<String, Object> extraClaims, final UserDetails userDetails) {
        return Jwts.builder().claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuer(ISSUER)
                .issuedAt(nowDate())
                .expiration(nowDatePlus(24, TimeUnit.MINUTES))
                .signWith(signingKey).compact();
    }

    @Override
    public String generateRefreshToken(final UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuer(ISSUER)
                .signWith(signingKey).compact();
    }

    // private methods
    private static Date nowDate() {
        final Instant now = Instant.now();
        return Date.from(now);
    }

    private static Date nowDatePlus(final long duration, final TimeUnit timeUnit) {
        final Instant now = Instant.now();
        return Date.from(now.plus(duration, timeUnit.toChronoUnit()));
    }

    private <T> T extractClaim(final String token, final Function<Claims, T> claimsResolvers) {
        final Claims claims = extractAllClaims(token);
        return claimsResolvers.apply(claims);
    }

    private Date extractExpiration(final String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(final String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }
}
