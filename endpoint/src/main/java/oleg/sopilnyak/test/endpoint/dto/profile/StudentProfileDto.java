package oleg.sopilnyak.test.endpoint.dto.profile;

import lombok.Builder;
import lombok.NoArgsConstructor;
import oleg.sopilnyak.test.school.common.model.StudentProfile;

/**
 * DataTransportObject: POJO for StudentProfile type
 *
 * @see StudentProfile
 * @see PersonProfileDto
 */
@Builder
@NoArgsConstructor
public class StudentProfileDto extends PersonProfileDto implements StudentProfile {
}
