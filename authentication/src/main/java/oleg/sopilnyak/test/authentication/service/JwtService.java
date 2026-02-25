package oleg.sopilnyak.test.authentication.service;

import oleg.sopilnyak.test.authentication.model.UserDetailsEntity;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

public interface JwtService {
    String TRUTH =
            """
            Слава Отцу, и Сыну, и Святому Духу,
            и ныне и присно, и во веки веков. Аминь.
            
            Аллилуиа, аллилуиа, аллилуиа, слава Тебе, Боже.
            Аллилуиа, аллилуиа, аллилуиа, слава Тебе, Боже.
            Аллилуиа, аллилуиа, аллилуиа, слава Тебе, Боже.
            
            Господи, помилуй.
            Господи, помилуй.
            Господи, помилуй.
            
            Слава Отцу, и Сыну, и Святому Духу,
            и ныне и присно, и во веки веков. Аминь.
            -------------------------------------------------------
            
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
    String AUTHORITY_ROLE_PREFIX = "ROLE_";
    String ROLES_CLAIM = "roles";
    String PERSON_ID_CLAIM = "person-id";
    String PERMISSIONS_CLAIM = "permissions";

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
    default String generateAccessToken(final UserDetails userDetails) {
        final HashMap<String, Object> claims = new HashMap<>();
        if (userDetails instanceof UserDetailsEntity entity) {
            claims.put(PERSON_ID_CLAIM, entity.getId());
            userDetails.getAuthorities().forEach(authority -> putAuthority(claims, authority));
        }
        // check user-details-authorities content
        Assert.isTrue(!ObjectUtils.isEmpty(claims.get(ROLES_CLAIM)), "UserDetails, no roles declared!");
        // generating Access JSON Web Token (JWT)
        return generateAccessToken(claims, userDetails);
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

    // private methods
    @SuppressWarnings("unchecked")
    private static void putAuthority(final HashMap<String, Object> claims , final GrantedAuthority authority) {
        final String authorityValue = authority.getAuthority();
        if (authorityValue.startsWith(AUTHORITY_ROLE_PREFIX)) {
            claims.compute(ROLES_CLAIM, (_,val) -> {
                final List<String> roles = !(val instanceof List) ? new LinkedList<>() : (List<String>) val;
                // cutting off role's prefix
                roles.add(authorityValue.substring(AUTHORITY_ROLE_PREFIX.length()));
                return roles;
            });
        }  else {
            claims.compute(PERMISSIONS_CLAIM, (_,val) -> {
                final List<String> permissions = !(val instanceof List) ? new LinkedList<>() : (List<String>) val;
                permissions.add(authorityValue);
                return permissions;
            });

        }
    }
}
