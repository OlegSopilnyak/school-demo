package oleg.sopilnyak.test.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;

/**
 * DataTransportObject: POJO for PrincipalProfile type
 *
 * @see PrincipalProfile
 * @see BaseProfileDto
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, doNotUseGetters = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrincipalProfileDto extends BaseProfileDto implements PrincipalProfile {
    // user-name for principal person's login
    private String login;
}
