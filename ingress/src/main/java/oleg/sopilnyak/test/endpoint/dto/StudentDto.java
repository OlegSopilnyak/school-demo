package oleg.sopilnyak.test.endpoint.dto;

import oleg.sopilnyak.test.school.common.model.education.Course;
import oleg.sopilnyak.test.school.common.model.education.Student;

import java.util.List;
import org.hibernate.validator.constraints.UniqueElements;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * DataTransportObject: POJO for Student type
 *
 * @see Student
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, doNotUseGetters = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentDto extends BasePersonDto implements Student {
    private String description;

    @UniqueElements(message = "Courses should be unique")
    @JsonDeserialize(contentAs = CourseDto.class)
    private List<Course> courses;
}
