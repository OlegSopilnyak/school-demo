package oleg.sopilnyak.test.service.message.payload;

import oleg.sopilnyak.test.school.common.model.PrincipalProfile;

import java.security.NoSuchAlgorithmException;
import org.springframework.util.ObjectUtils;
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
    // user-name for principal person's login
    private String login;
    // signature for login + password string
    private String signature;

    /**
     * To check is it the correct password for login
     *
     * @param password password to check
     * @return true if password is correct
     */
    @Override
    public boolean isPassword(String password) {
        if (ObjectUtils.isEmpty(signature) || ObjectUtils.isEmpty(login)) {
            return false;
        }
        try {
            return signature.equals(makeSignatureFor(password));
        } catch (NoSuchAlgorithmException _) {
            return false;
        }
    }
}
