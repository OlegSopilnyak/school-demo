package oleg.sopilnyak.test.service.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;

import java.util.List;

/**
 * BusinessMessage Payload Type: POJO for StudentGroup type
 *
 * @see StudentsGroup
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
public class StudentsGroupPayload extends BasePayload<StudentsGroup> implements StudentsGroup {
    private String name;

    @JsonDeserialize(as= StudentPayload.class)
    private Student leader;
    @JsonDeserialize(contentAs= StudentPayload.class)
    private List<Student> students;
}
