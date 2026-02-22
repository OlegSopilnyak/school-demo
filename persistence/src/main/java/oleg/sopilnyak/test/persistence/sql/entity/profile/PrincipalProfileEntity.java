package oleg.sopilnyak.test.persistence.sql.entity.profile;

import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.authentication.Role;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.hibernate.proxy.HibernateProxy;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * DatabaseEntity: Entity for PrincipalProfile type
 *
 * @see PrincipalProfile
 * @see PersonProfileEntity
 */
@Getter
@Setter
@RequiredArgsConstructor
@SuperBuilder
@ToString(callSuper = true)

@Entity
@DiscriminatorValue("0")
public class PrincipalProfileEntity extends PersonProfileEntity implements PrincipalProfile {
    @Column(unique = true, columnDefinition = "varchar(50) default 'Not a Principal'")
    private String username;
    @Column(columnDefinition = "varchar(150) default 'Not a Principal'")
    private String signature;

    @Column(name = "person_role", columnDefinition = "varchar(50) default 'STUDENT'")
    @Enumerated(EnumType.STRING)
    private Role role;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "person_permissions")
    @CollectionTable(name = "person_permissions", joinColumns = @JoinColumn(name = "permission", referencedColumnName = "username"))
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        PrincipalProfileEntity that = (PrincipalProfileEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
