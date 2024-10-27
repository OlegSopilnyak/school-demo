package oleg.sopilnyak.test.endpoint.dto.education;

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
 * DataTransportObject: POJO for Course type
 *
 * @see Course
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseDto implements Course {
    private Long id;
    private String name;
    private String description;
    @JsonDeserialize(contentAs= StudentDto.class)
    private List<Student> students;
}
