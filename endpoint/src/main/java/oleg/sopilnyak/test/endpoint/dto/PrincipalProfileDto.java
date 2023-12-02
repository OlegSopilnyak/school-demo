package oleg.sopilnyak.test.endpoint.dto;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;


/**
 * DataTransportObject: POJO for PrincipalProfile type
 *
 * @see PrincipalProfile
 * @see PersonProfileDto
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class PrincipalProfileDto extends PersonProfileDto implements PrincipalProfile {
    // user-name for principal person's login
    private String login;
}
