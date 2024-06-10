package oleg.sopilnyak.test.service.message;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import oleg.sopilnyak.test.school.common.model.StudentProfile;

/**
 * BusinessMessage Payload Type: POJO for StudentProfile type
 *
 * @see StudentProfile
 * @see BaseProfilePayload
 */
@Builder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, doNotUseGetters = true)
public class StudentProfilePayload extends BaseProfilePayload<StudentProfile> implements StudentProfile {
}
