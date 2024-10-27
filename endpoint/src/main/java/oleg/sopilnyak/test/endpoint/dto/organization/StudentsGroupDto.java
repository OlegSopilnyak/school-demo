package oleg.sopilnyak.test.endpoint.dto.organization;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import oleg.sopilnyak.test.endpoint.dto.education.StudentDto;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;

import java.util.List;

/**
 * DataTransportObject: POJO for StudentGroup type
 *
 * @see StudentsGroup
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentsGroupDto implements StudentsGroup {
    private Long id;
    private String name;

    @JsonDeserialize(as= StudentDto.class)
    private Student leader;
    @JsonDeserialize(contentAs= StudentDto.class)
    private List<Student> students;
}
