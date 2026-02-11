package oleg.sopilnyak.test.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;
import lombok.experimental.SuperBuilder;
import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.school.common.model.organization.StudentsGroup;

import java.util.List;

/**
 * DataTransportObject: POJO for StudentGroup type
 *
 * @see StudentsGroup
 * @see Student
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, doNotUseGetters = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentsGroupDto extends BaseDto implements StudentsGroup {
    private String name;

    @JsonDeserialize(as = StudentDto.class)
    private Student leader;
    @JsonDeserialize(contentAs = StudentDto.class)
    private List<Student> students;
}
