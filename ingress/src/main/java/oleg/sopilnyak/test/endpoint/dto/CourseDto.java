package oleg.sopilnyak.test.endpoint.dto;

import oleg.sopilnyak.test.school.common.model.education.Course;
import oleg.sopilnyak.test.school.common.model.education.Student;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
 * DataTransportObject: POJO for Course type
 *
 * @see Course
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, doNotUseGetters = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CourseDto extends BaseDto implements Course {
    @NotNull
    @NotBlank(message = "Name cannot be blank")
    private String name;
    private String description;

    @UniqueElements(message = "Students should be unique")
    @JsonDeserialize(contentAs = StudentDto.class)
    private List<Student> students;
}
