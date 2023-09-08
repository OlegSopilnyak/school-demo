package oleg.sopilnyak.test.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class AuthorityPersonDto implements AuthorityPerson {
    private Long id;
    private String title;
    private String firstName;
    private String lastName;
    private String gender;
}
