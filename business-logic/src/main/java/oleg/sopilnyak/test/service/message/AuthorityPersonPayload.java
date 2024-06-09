package oleg.sopilnyak.test.service.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.*;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Faculty;

import java.util.List;

import static org.springframework.util.ObjectUtils.isEmpty;


/**
 * BusinessMessage Payload Type: POJO for AuthorityPerson type
 *
 * @see AuthorityPerson
 * @see Faculty
 * @see FacultyPayload
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, doNotUseGetters = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorityPersonPayload extends BasePersonPayload<AuthorityPerson> implements AuthorityPerson {
    private String title;

    @ToString.Exclude
    @JsonDeserialize(contentAs = FacultyPayload.class)
    private List<Faculty> faculties;

    @ToString.Include
    String faculties() {
        return " '" + (isEmpty(faculties) ? 0 : faculties.size()) +" items'";
    }
}
