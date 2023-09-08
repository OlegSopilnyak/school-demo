package oleg.sopilnyak.test.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Faculty;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FacultyDto implements Faculty {
    private Long id;
    private String name;

    @JsonDeserialize(as = AuthorityPersonDto.class)
    private AuthorityPerson dean;
    @JsonDeserialize(contentAs = CourseDto.class)
    private List<Course> courses;
}
