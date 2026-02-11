package oleg.sopilnyak.test.service.message.payload;

import static org.springframework.util.ObjectUtils.isEmpty;

import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.organization.Faculty;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;


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
@SuperBuilder
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
