package oleg.sopilnyak.test.service.message.payload;

import static org.springframework.util.ObjectUtils.isEmpty;

import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Faculty;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;


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
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, doNotUseGetters = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FacultyPayload extends BasePayload<Faculty> implements Faculty {
    private String name;

    @JsonDeserialize(as = AuthorityPersonPayload.class)
    private AuthorityPerson dean;

    @JsonDeserialize(contentAs = CoursePayload.class)
    @ToString.Exclude
    private List<Course> courses;

    @ToString.Include(name = "courses")
    private String itemsCapacity() {
        return String.format(ITEMS_CAPACITY_FORMAT, isEmpty(courses) ? 0 : courses.size());
    }
}
