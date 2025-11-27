package oleg.sopilnyak.test.service.message.payload;

import oleg.sopilnyak.test.school.common.model.BaseType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

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
public abstract class BasePayload<T extends BaseType> implements BaseType {
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
