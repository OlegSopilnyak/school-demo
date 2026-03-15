package oleg.sopilnyak.test.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;
import oleg.sopilnyak.test.school.common.model.Person;
import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DataTransportObject: parent class for Student/AuthorityPerson model type
 *
 * @see Person
 * @see Student
 * @see AuthorityPerson
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
    @NotNull
    @NotBlank(message = "First name cannot be blank")
    private String firstName;
    @NotNull
    @NotBlank(message = "Last name cannot be blank")
    private String lastName;
    @NotNull
    @NotBlank(message = "Gender cannot be blank")
    private String gender;
}
