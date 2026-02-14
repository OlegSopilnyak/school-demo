package oleg.sopilnyak.test.persistence.sql.entity.profile;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.authentication.Role;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

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
    @NotNull
    @Column(unique = true)
    private String username;
    @NotNull
    private String signature;

    @Column(name = "person_role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Role role;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "person_permissions", nullable = false)
    @CollectionTable(name = "person_permissions", joinColumns = @JoinColumn(name = "permission", referencedColumnName = "username"))
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
}
