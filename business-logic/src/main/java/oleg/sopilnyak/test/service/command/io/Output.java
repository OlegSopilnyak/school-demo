package oleg.sopilnyak.test.service.command.io;

import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.service.command.io.result.BooleanResult;
import oleg.sopilnyak.test.service.command.io.result.EmptyResult;
import oleg.sopilnyak.test.service.command.io.result.NumberIdResult;
import oleg.sopilnyak.test.service.command.io.result.OptionalValueResult;
import oleg.sopilnyak.test.service.command.io.result.PayloadResult;
import oleg.sopilnyak.test.service.command.io.result.PayloadSetResult;
import oleg.sopilnyak.test.service.command.io.result.StringIdResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.message.CommandMessage;
import oleg.sopilnyak.test.service.message.payload.BasePayload;

import java.io.IOException;
import java.util.Optional;
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
     * To create string result output
     *
     * @return new instance of the output
     * @see StringIdResult
     */
    static Output<String> of(final String result) {
        return new StringIdResult(result);
    }

    /**
     * To create number result output
     *
     * @return new instance of the output
     * @see NumberIdResult
     */
    static Output<Number> of(final Number result) {
        return new NumberIdResult<>(result);
    }


    /**
     * To create optional result output
     *
     * @return new instance of the output
     * @see OptionalValueResult
     */
    static <T> Output<Optional<T>> of (final Optional<T> result) {
        return new OptionalValueResult<>(result);
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
        return switch (result) {
            case null -> empty();
            // primitive output types
            case Boolean booleanResult -> of(booleanResult);
            case String stringResult -> of(stringResult);
            case Number numberResult -> of(numberResult);
            case Optional<?> optionalResult -> of(optionalResult);
            // payload output types
            case BasePayload<?> payloadResult -> of(payloadResult);
            case Set<?> payloadSetResult -> of(payloadSetResult);
            // unknown result type
            default -> throw new IllegalArgumentException("Output result type isn't supported: " + result.getClass());
        };
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
    class ResultDeserializer<R> extends StdDeserializer<Output<R>> {

        public ResultDeserializer() {
            this(Output.class);
        }

        protected ResultDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public Output<R> deserialize(final JsonParser jsonParser, final DeserializationContext ignored) throws IOException {
            final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
            final TreeNode resultNode = jsonParser.readValueAsTree();
            final var resultType = IOBase.restoreIoBaseClass(resultNode, Output.class);
            final var javaResultType = mapper.getTypeFactory().constructType(resultType);
            return mapper.readValue(resultNode.toString(), javaResultType);
        }
    }
}
