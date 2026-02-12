package oleg.sopilnyak.test.authentication.service;

import java.util.Map;
import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {
    String ISSUER = "Basic School Application";

    String extractUserName(String token);


    /**
     * To generate JWT for user-details only (without any claim)
     *
     * @param userDetails user-details of the token
     * @return generated token based on user-details
     * @see UserDetails
     */
    default String generateToken(UserDetails userDetails) {
        return generateToken(Map.of(), userDetails);
    }

    /**
     * To generate refresh JWT for user-details
     *
     * @param userDetails user-details of the token
     * @return generated token based on user-details
     * @see UserDetails
     */
    String generateRefreshToken(UserDetails userDetails);

    /**
     * To generate JWT for user-details and claims
     *
     * @param claims the claims of the token
     * @param userDetails user-details of the token
     * @return generated token based on user-details and claims
     * @see UserDetails
     */
    String generateToken(Map<String, Object> claims, UserDetails userDetails);

    boolean isTokenValid(String token, UserDetails userDetails);
}
