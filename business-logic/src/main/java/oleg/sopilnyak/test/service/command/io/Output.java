package oleg.sopilnyak.test.service.command.io;

import static java.util.Objects.isNull;

import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.service.command.io.result.BooleanResult;
import oleg.sopilnyak.test.service.command.io.result.EmptyResult;
import oleg.sopilnyak.test.service.command.io.result.PayloadResult;
import oleg.sopilnyak.test.service.command.io.result.PayloadSetResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.message.CommandMessage;
import oleg.sopilnyak.test.service.message.payload.BasePayload;

import java.io.IOException;
import java.util.Set;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;


/**
 * Type: I/O school-command execution result
 *
 * @param <O> the type of command execution result value
 * @see oleg.sopilnyak.test.service.command.type.base.RootCommand#executeDo(Context)
 * @see Context#setResult(Object)
 */
public interface Output<O> extends IOBase<O> {

    /**
     * To create empty result output
     *
     * @return new instance of the output
     * @see EmptyResult
     */
    static <T> Output<T> empty() {
        return new EmptyResult<>();
    }

    /**
     * To create boolean result output
     *
     * @return new instance of the output
     * @see BooleanResult
     */
    static Output<Boolean> of(final Boolean result) {
        return new BooleanResult(result);
    }

    /**
     * To create payload result output
     *
     * @return new instance of the output
     * @see PayloadResult
     * @see BasePayload
     * @see BaseType
     */
    static <P extends BasePayload<? extends BaseType>> Output<P> of(final P result) {
        return new PayloadResult<>(result);
    }

    /**
     * To create payload-set result output
     *
     * @return new instance of the output
     * @see PayloadSetResult
     * @see Set
     * @see BasePayload
     * @see BaseType
     */
    static <P extends BasePayload<? extends BaseType>> Output<Set<P>> of(final Set<P> result) {
        return new PayloadSetResult<>(result);
    }

    /**
     * To create result output by result type
     *
     * @param result instance to wrap
     * @return new instance of the output
     * @see Output#of(Boolean)
     * @see Output#of(BasePayload)
     * @see Output#of(Set)
     * @see Output#empty()
     */
    static Output<?> of(final Object result) {
        if (isNull(result)) return empty();
        if (result instanceof Boolean booleanResult) return of(booleanResult);
        if (result instanceof BasePayload<?> payloadResult) return of(payloadResult);
        if (result instanceof Set<?> payloadSetResult) return of(payloadSetResult);
        throw new IllegalArgumentException("Result type is not supported: " + result.getClass());
    }

    // inner classes for JSON serializing/deserializing

    /**
     * JSON: Deserializer for Output Result field of the command-message
     *
     * @see StdDeserializer
     * @see CommandMessage#getContext()
     * @see Context#getResult()
     * @see Output
     */
    class ResultDeserializer<R> extends StdDeserializer<Output<?>> {

        public ResultDeserializer() {
            this(Output.class);
        }

        protected ResultDeserializer(Class<? extends Output> vc) {
            super(vc);
        }

        @Override
        @SuppressWarnings("unchecked")
        public Output<R> deserialize(final JsonParser jsonParser,
                                     final DeserializationContext deserializationContext) throws IOException {
            final TreeNode resultNode = jsonParser.readValueAsTree();
            final Class<? extends Output> inputParameterClass = IOBase.restoreIoBaseClass(resultNode, Output.class);
            return ((ObjectMapper) jsonParser.getCodec()).readValue(resultNode.toString(), inputParameterClass);
        }

    }
}
