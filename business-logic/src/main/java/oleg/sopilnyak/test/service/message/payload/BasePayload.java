package oleg.sopilnyak.test.service.message.payload;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;
import oleg.sopilnyak.test.school.common.model.BaseType;

/**
 * BusinessMessage Payload Type: POJO as parent of any payload
 *
 * @see BaseType
 */

@SuperBuilder
@ToString(doNotUseGetters = true)
@EqualsAndHashCode(exclude = {"original"})
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class BasePayload<T extends BaseType> {
    public static final String ITEMS_CAPACITY_FORMAT = " '%d items'";
    @Getter
    @Setter
    private Long id;

    // the type (class) of the original
    private String originalType;
    // the reference to original data
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

    public String getOriginalType() {
        return original instanceof BasePayload<?> payload ? payload.getOriginalType() : originalType;
    }
}
