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
    
    @Override
    public String extractUserName(final String token) {
        return token == null || token.isBlank() ? null : extractClaim(token, Claims::getSubject);
    }

    @Override
    public boolean isTokenValid(final String token, final UserDetails userDetails) {
        final String userName = extractUserName(token);
        return (userName.equals(userDetails.getUsername())) && !isTokenExpired(token);
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
