package oleg.sopilnyak.test.service.message.payload;

import oleg.sopilnyak.test.school.common.model.Person;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * BusinessMessage Payload Type: POJO for School Person (students | authority)
 *
 * @see AuthorityPersonPayload
 * @see StudentPayload
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true, doNotUseGetters = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BasePersonPayload<T extends Person> extends BasePayload<T> implements Person {
    private Long profileId;
    private String firstName;
    private String lastName;
    private String gender;
}
