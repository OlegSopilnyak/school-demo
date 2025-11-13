package oleg.sopilnyak.test.persistence.sql.entity.profile;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;

import jakarta.persistence.*;

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
public class PrincipalProfileEntity extends PersonProfileEntity implements PrincipalProfile {
    @Column(unique = true)
    private String login;
    private String signature;
}
