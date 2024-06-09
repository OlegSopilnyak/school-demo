package oleg.sopilnyak.test.service.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import oleg.sopilnyak.test.school.common.model.base.BaseType;

/**
 * BusinessMessage Payload Type: POJO as parent of any payload
 *
 * @see BaseType
 */

@Getter
@ToString(doNotUseGetters = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BasePayload<T extends BaseType> {
    @Setter
    private Long id;
    // the type (class) of the original
    private String originalType;
    @JsonIgnore
    private T original;

    public T getOriginal() {
        if (original instanceof BasePayload<?> payload){
            return (T) payload.getOriginal();
        }
        return original;
    }

    public void setOriginal(T original) {
        this.originalType = original.getClass().getName();
        this.original = original;
    }
}
