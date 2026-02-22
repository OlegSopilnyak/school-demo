package oleg.sopilnyak.test.persistence.sql.entity.profile;

import oleg.sopilnyak.test.school.common.model.person.profile.StudentProfile;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.util.Objects;
import org.hibernate.proxy.HibernateProxy;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * DatabaseEntity: Entity for StudentProfile type
 *
 * @see StudentProfile
 * @see PersonProfileEntity
 */
@SuperBuilder
@NoArgsConstructor
@ToString(callSuper = true)

@Entity
@DiscriminatorValue("2")
public class StudentProfileEntity extends PersonProfileEntity implements StudentProfile {
    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        StudentProfileEntity that = (StudentProfileEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy hibernateProxy ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
