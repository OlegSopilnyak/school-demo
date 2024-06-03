package oleg.sopilnyak.test.service.message;

import lombok.Builder;
import lombok.NoArgsConstructor;
import oleg.sopilnyak.test.school.common.model.StudentProfile;

/**
 * BusinessMessage Payload Type: POJO for StudentProfile type
 *
 * @see StudentProfile
 * @see BasePersonProfile
 */
@Builder
@NoArgsConstructor
public class StudentProfilePayload extends BasePersonProfile implements StudentProfile {
}
