package oleg.sopilnyak.test.service.message;

import lombok.*;
import lombok.experimental.SuperBuilder;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;


/**
 * BusinessMessage Payload Type: POJO for PrincipalProfile type
 *
 * @see PrincipalProfile
 * @see BaseProfilePayload
 */
@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, doNotUseGetters = true)
public class PrincipalProfilePayload extends BaseProfilePayload<PrincipalProfile> implements PrincipalProfile {
    // user-name for principal person's login
    private String login;
    // signature for login + password string
    private String signature;
}
