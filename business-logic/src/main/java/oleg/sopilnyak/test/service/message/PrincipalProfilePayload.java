package oleg.sopilnyak.test.service.message;

import lombok.*;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;


/**
 * BusinessMessage Payload Type: POJO for PrincipalProfile type
 *
 * @see PrincipalProfile
 * @see BaseProfilePayload
 */
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, doNotUseGetters = true)
public class PrincipalProfilePayload extends BaseProfilePayload<PrincipalProfile> implements PrincipalProfile {
    // user-name for principal person's login
    private String login;
}
