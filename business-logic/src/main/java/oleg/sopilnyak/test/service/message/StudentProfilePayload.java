package oleg.sopilnyak.test.service.message;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import oleg.sopilnyak.test.school.common.model.StudentProfile;

/**
 * BusinessMessage Payload Type: POJO for StudentProfile type
 *
 * @see StudentProfile
 * @see BaseProfilePayload
 */
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, doNotUseGetters = true)
public class StudentProfilePayload extends BaseProfilePayload<StudentProfile> implements StudentProfile {
}
