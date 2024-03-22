package oleg.sopilnyak.test.persistence.sql.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.xml.bind.DatatypeConverter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * DatabaseEntity: Entity for PrincipalProfile type
 *
 * @see PrincipalProfile
 * @see PersonProfileEntity
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)

@Entity
@DiscriminatorValue("0")
@Table(indexes = @Index(name = "principal_login", columnList = "login", unique = true))
public class PrincipalProfileEntity extends PersonProfileEntity implements PrincipalProfile {
    private String login;
    @Getter(AccessLevel.NONE)
    private String signature;

    /**
     * To get user-name for principal person's login
     *
     * @return value of user-name
     */
    @Override
    public String getLogin() {
        return login;
    }

    /**
     * To check is it the correct password for login
     *
     * @param password password to check
     * @return true if password is correct
     */
    @Override
    public boolean isPassword(String password) {
        final String toSign = login + " " + password;
        try {
            final MessageDigest md = MessageDigest.getInstance("MD5");
            final byte[] digest = md.digest(toSign.getBytes());
            final String mySignature = DatatypeConverter.printHexBinary(digest).toUpperCase();
            return mySignature.equals(signature);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }
}
