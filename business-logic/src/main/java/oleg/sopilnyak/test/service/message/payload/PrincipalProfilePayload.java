package oleg.sopilnyak.test.service.message.payload;

import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.authentication.Role;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;

import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Set;
import org.springframework.util.ObjectUtils;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;


/**
 * BusinessMessage Payload Type: POJO for PrincipalProfile type
 *
 * @see PrincipalProfile
 * @see BaseProfilePayload
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, doNotUseGetters = true)
public class PrincipalProfilePayload extends BaseProfilePayload<PrincipalProfile> implements PrincipalProfile {
    // user-name for principal person's sign in
    private String username;
    // signature for login + password string
    private String signature;
    // principal person role in the school
    private Role role;
    // principal person permissions in the school activities
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    /**
     * To check is it the correct password for login
     *
     * @param password password to check
     * @return true if password is correct
     */
    @Override
    public boolean isPassword(String password) {
        if (ObjectUtils.isEmpty(signature) || ObjectUtils.isEmpty(username)) {
            return false;
        }
        try {
            return signature.equals(makeSignatureFor(password));
        } catch (NoSuchAlgorithmException _) {
            return false;
        }
    }
}
