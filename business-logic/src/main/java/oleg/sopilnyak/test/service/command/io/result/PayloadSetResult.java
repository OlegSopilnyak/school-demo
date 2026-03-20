package oleg.sopilnyak.test.service.command.io.result;

import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.service.command.io.IOBase;
import oleg.sopilnyak.test.service.command.io.Output;
import oleg.sopilnyak.test.service.message.payload.BasePayload;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.ObjectUtils;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * Type: I/O school-command set of Model-types command execution result
 *
 * @see Set
 * @see BasePayload
 * @see BaseType
 * @see Output
 */
@JsonSerialize(using = PayloadSetResult.Serializer.class)
@JsonDeserialize(using = ResultsContainer.Deserializer.class)
public final class PayloadSetResult<P extends BasePayload<? extends BaseType>>
        extends ResultsContainer<P> implements Output<Set<P>> {

    public PayloadSetResult(Collection<P> valuesSet) {
        super(valuesSet);
    }

    public Set<P> value() {
        // keeping the order as in the nest
        return Arrays.stream(nest).map(IOBase::value).collect(Collectors.toCollection(LinkedHashSet::new));
    }


    /**
     * JSON: Serializer for PayloadSetResult
     *
     * @see ResultsContainer.Serializer
     * @see PayloadSetResult
     */
    static class Serializer extends ResultsContainer.Serializer {
        /**
         * To check the nest before serialization
         *
         * @param results array of results (the nest) to check before
         * @throws IOException if the nest cannot be serialized
         */
        @Override
        protected <T> void checkBeforeSerialize(final Output<T>[] results) throws IOException {
            if (!ObjectUtils.isEmpty(results)) {
                final Class<?> firstElementInNestType = results[0].value().getClass();
                for (final Output<T> result: results) {
                    if (!Objects.equals(firstElementInNestType, result.value().getClass())) {
                        throw new IOException("Payload Set parameter elements types mismatch");
                    }
                }
            }
        }
    }
}
