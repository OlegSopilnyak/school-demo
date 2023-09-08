package oleg.sopilnyak.test.endpoint.dto;

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
 * DataTransportObject: POJO for Student type
 *
 * @see Student
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentDto implements Student {
    private Long id;
    private String firstName;
    private String lastName;
    private String gender;
    private String description;
    @JsonDeserialize(contentAs= CourseDto.class)
    private List<Course> courses;
}
