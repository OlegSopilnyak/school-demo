package oleg.sopilnyak.test.endpoint.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;
import oleg.sopilnyak.test.school.common.model.BaseType;

/**
 * DataTransportObject: parent class for any model type
 *
 * @see BaseType
 */
@Getter
@Setter
@SuperBuilder
@ToString(doNotUseGetters = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BaseDto implements BaseType {
    private Long id;
}
