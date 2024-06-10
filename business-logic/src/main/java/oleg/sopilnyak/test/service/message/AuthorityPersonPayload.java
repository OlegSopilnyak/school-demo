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

    @JsonDeserialize(contentAs = FacultyPayload.class)
    @ToString.Exclude
    private List<Faculty> faculties;

    @ToString.Include(name = "faculties")
    private String itemsCapacity() {
        return String.format(ITEMS_CAPACITY_FORMAT, isEmpty(faculties) ? 0 : faculties.size());
    }
}
