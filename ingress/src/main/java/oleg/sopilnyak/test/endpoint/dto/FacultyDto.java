package oleg.sopilnyak.test.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;
import lombok.experimental.SuperBuilder;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Faculty;

import java.util.List;
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
    private String name;

    @JsonDeserialize(as = AuthorityPersonDto.class)
    private AuthorityPerson dean;
    @JsonDeserialize(contentAs = CourseDto.class)
    private List<Course> courses;
}
