package oleg.sopilnyak.test.service.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentPayload implements Student {
    private Long id;
    private String firstName;
    private String lastName;
    private String gender;
    private String description;
    @JsonDeserialize(contentAs= CoursePayload.class)
    private List<Course> courses;
}
