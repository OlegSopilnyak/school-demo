package oleg.sopilnyak.test.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;
import lombok.experimental.SuperBuilder;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.organization.Faculty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.hibernate.validator.constraints.UniqueElements;

/**
 * DataTransportObject: POJO for AuthorityPerson type
 *
 * @see AuthorityPerson
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, doNotUseGetters = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorityPersonDto extends BasePersonDto implements AuthorityPerson {
    @NotNull
    @NotBlank(message = "Title cannot be blank")
    private String title;

    @UniqueElements(message = "Faculties should be unique")
    @JsonDeserialize(contentAs = FacultyDto.class)
    List<Faculty> faculties;
}
