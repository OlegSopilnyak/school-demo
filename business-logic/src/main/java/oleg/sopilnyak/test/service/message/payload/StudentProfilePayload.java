package oleg.sopilnyak.test.service.message.payload;

import oleg.sopilnyak.test.school.common.model.StudentProfile;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

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
