package oleg.sopilnyak.test.service.message.payload;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;
import lombok.experimental.SuperBuilder;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;

import java.util.List;

import static org.springframework.util.ObjectUtils.isEmpty;

/**
 * BusinessMessage Payload Type: POJO for StudentGroup type
 *
 * @see StudentsGroup
 * @see Student
 * @see StudentPayload
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, doNotUseGetters = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentsGroupPayload extends BasePayload<StudentsGroup> implements StudentsGroup {
    private String name;

    @JsonDeserialize(as= StudentPayload.class)
    private Student leader;

    @JsonDeserialize(contentAs= StudentPayload.class)
    @ToString.Exclude
    private List<Student> students;

    @ToString.Include(name = "students")
    private String itemsCapacity() {
        return String.format(ITEMS_CAPACITY_FORMAT, isEmpty(students) ? 0 : students.size());
    }
}
