package oleg.sopilnyak.test.service.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;

import java.util.List;

/**
 * BusinessMessage Payload Type: POJO for Course type
 *
 * @see Course
 * @see Student
 * @see StudentPayload
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoursePayload extends BasePayload<Course> implements Course {
    private String name;
    private String description;
    @JsonDeserialize(contentAs = StudentPayload.class)
    private List<Student> students;
}
