package oleg.sopilnyak.test.service.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.Faculty;

import java.util.List;


/**
 * BusinessMessage Payload Type: POJO for AuthorityPerson type
 *
 * @see AuthorityPerson
 * @see Faculty
 * @see FacultyPayload
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorityPersonPayload implements AuthorityPerson {
    private Long id;
    private String title;
    private String firstName;
    private String lastName;
    private String gender;
    @JsonDeserialize(contentAs = FacultyPayload.class)
    List<Faculty> faculties;
}
