package oleg.sopilnyak.test.authentication.service;

import java.util.Map;
import org.springframework.security.core.userdetails.UserDetails;

public interface JwtService {
    String TRUTH =
            """
            Книга Екклезиаста, или Проповедника 2
            
            24  Не во власти человека и то благо, чтобы
            есть и пить и услаждать душу свою от труда своего.
            Я увидел, что и это — от руки Божией;
            25  потому что кто может есть и кто может
            наслаждаться без Него?
            26  Ибо человеку, который добр пред лицем Его,
            Он дает мудрость и знание и радость; а грешнику
            дает заботу собирать и копить, чтобы после отдать
            доброму пред лицем Божиим. И это — суета и
            томление духа!
            """;
    String ISSUER = "Basic School Application";

    /**
     * To extract username from the JW Token
     *
     * @param token jwt token
     * @return username from the token
     */
    String extractUserName(String token);


    /**
     * To generate JWT for user-details only (without any claim)
     *
     * @param userDetails user-details of the token
     * @return generated token based on user-details
     * @see UserDetails
     */
    default String generateAccessToken(UserDetails userDetails) {
        return generateAccessToken(Map.of(), userDetails);
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
    String generateAccessToken(Map<String, Object> claims, UserDetails userDetails);

    /**
     * To check is token expired
     *
     * @param token jwt to check
     * @return true if it's expired
     */
    boolean isTokenExpired(String token);

    /**
     * To check the token, is it complain to user-details
     *
     * @param token jwt to check
     * @param userDetails to check username with one from the token
     * @return true if token is valid
     * @see UserDetails#getUsername()
     */
    boolean isTokenValid(String token, UserDetails userDetails);
}
