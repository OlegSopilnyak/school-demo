package oleg.sopilnyak.test.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;

import oleg.sopilnyak.test.school.common.model.authentication.Permission;
import oleg.sopilnyak.test.school.common.model.authentication.Role;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;

import java.util.HashSet;
import java.util.Set;

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
    // user-name for principal person's sign in
    private String username;
    // principal person role in the school
    private Role role;
    // principal person permissions in the school activities
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
}
