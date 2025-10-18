package oleg.sopilnyak.test.persistence.sql.entity.profile;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Index;
import javax.persistence.Table;

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
@Table(indexes = {
        @Index(name = "principal_login", columnList = "login", unique = true)
})
public class PrincipalProfileEntity extends PersonProfileEntity implements PrincipalProfile {
    private String login;
    private String signature;
}
