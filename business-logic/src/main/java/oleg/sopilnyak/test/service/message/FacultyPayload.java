package oleg.sopilnyak.test.service.message;

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


/**
 * BusinessMessage Payload Type: POJO for Faculty type
 *
 * @see Faculty
 * @see AuthorityPerson
 * @see AuthorityPersonPayload
 * @see Course
 * @see CoursePayload
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FacultyPayload implements Faculty {
    private Long id;
    private String name;

    @JsonDeserialize(as = AuthorityPersonPayload.class)
    private AuthorityPerson dean;
    @JsonDeserialize(contentAs = CoursePayload.class)
    private List<Course> courses;
}
