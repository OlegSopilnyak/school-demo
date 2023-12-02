package oleg.sopilnyak.test.endpoint.dto;

import lombok.Builder;
import oleg.sopilnyak.test.school.common.model.StudentProfile;

/**
 * DataTransportObject: POJO for StudentProfile type
 *
 * @see StudentProfile
 * @see PersonProfileDto
 */
@Builder
public class StudentProfileDto extends PersonProfileDto implements StudentProfile {
}
