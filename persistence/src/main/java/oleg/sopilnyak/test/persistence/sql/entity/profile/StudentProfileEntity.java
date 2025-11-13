package oleg.sopilnyak.test.persistence.sql.entity.profile;

import lombok.*;
import lombok.experimental.SuperBuilder;
import oleg.sopilnyak.test.school.common.model.StudentProfile;

import jakarta.persistence.*;

/**
 * DatabaseEntity: Entity for StudentProfile type
 *
 * @see StudentProfile
 * @see PersonProfileEntity
 */
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)

@Entity
@DiscriminatorValue("2")
public class StudentProfileEntity extends PersonProfileEntity implements StudentProfile {
}
