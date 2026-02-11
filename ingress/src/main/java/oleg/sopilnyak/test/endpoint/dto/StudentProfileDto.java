package oleg.sopilnyak.test.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;
import oleg.sopilnyak.test.school.common.model.person.profile.StudentProfile;

/**
 * DataTransportObject: POJO for StudentProfile type
 *
 * @see StudentProfile
 * @see BaseProfileDto
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, doNotUseGetters = true)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentProfileDto extends BaseProfileDto implements StudentProfile {
}
