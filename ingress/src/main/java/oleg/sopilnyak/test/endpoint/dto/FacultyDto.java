package oleg.sopilnyak.test.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;
import lombok.experimental.SuperBuilder;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.education.Course;
import oleg.sopilnyak.test.school.common.model.organization.Faculty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.hibernate.validator.constraints.UniqueElements;

/**
 * DataTransportObject: POJO for Faculty type
 *
 * @see Faculty
 * @see AuthorityPerson
 * @see Course
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, doNotUseGetters = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FacultyDto extends BaseDto implements Faculty {
    @NotNull
    @NotBlank(message = "Name cannot be blank")
    private String name;

    @JsonDeserialize(as = AuthorityPersonDto.class)
    private AuthorityPerson dean;
    @UniqueElements(message = "Courses should be unique")
    @JsonDeserialize(contentAs = CourseDto.class)
    private List<Course> courses;
}
