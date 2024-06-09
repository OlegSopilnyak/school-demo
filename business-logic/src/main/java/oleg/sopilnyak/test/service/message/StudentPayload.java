package oleg.sopilnyak.test.service.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;

import java.util.List;

/**
 * BusinessMessage Payload Type: POJO for Student type
 *
 * @see Student
 * @see Course
 * @see CoursePayload
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentPayload extends BasePersonPayload<Student> implements Student {
    private String description;

    @JsonDeserialize(contentAs= CoursePayload.class)
    private List<Course> courses;
}
