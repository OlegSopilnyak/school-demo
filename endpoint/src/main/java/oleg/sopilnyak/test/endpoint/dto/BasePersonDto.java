package oleg.sopilnyak.test.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;
import oleg.sopilnyak.test.school.common.model.Person;

/**
 * DataTransportObject: parent class for Student/AuthorityPerson model type
 *
 * @see Person
 * @see oleg.sopilnyak.test.school.common.model.Student
 * @see oleg.sopilnyak.test.school.common.model.AuthorityPerson
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, doNotUseGetters = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BasePersonDto extends BaseDto implements Person {
    private Long profileId;
    private String firstName;
    private String lastName;
    private String gender;
}
