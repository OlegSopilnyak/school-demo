package oleg.sopilnyak.test.persistence.sql.entity;

import lombok.*;
import lombok.experimental.SuperBuilder;
import oleg.sopilnyak.test.school.common.model.StudentProfile;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

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
