package oleg.sopilnyak.test.service.command.io;

import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.school.common.model.education.Course;
import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.organization.Faculty;
import oleg.sopilnyak.test.school.common.model.organization.StudentsGroup;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.person.profile.StudentProfile;
import oleg.sopilnyak.test.service.command.io.parameter.PayloadParameter;
import oleg.sopilnyak.test.service.command.io.result.BooleanResult;
import oleg.sopilnyak.test.service.command.io.result.CompositeOutputParameter;
import oleg.sopilnyak.test.service.command.io.result.EmptyResult;
import oleg.sopilnyak.test.service.command.io.result.NumberIdResult;
import oleg.sopilnyak.test.service.command.io.result.OptionalValueResult;
import oleg.sopilnyak.test.service.command.io.result.PayloadResult;
import oleg.sopilnyak.test.service.command.io.result.PayloadSetResult;
import oleg.sopilnyak.test.service.command.io.result.StringIdResult;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.CommandMessage;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;
import oleg.sopilnyak.test.service.message.payload.BasePayload;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;
import oleg.sopilnyak.test.service.message.payload.FacultyPayload;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;
import oleg.sopilnyak.test.service.message.payload.StudentsGroupPayload;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.mockito.internal.util.MockUtil;
import org.springframework.util.CollectionUtils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.mapstruct.factory.Mappers;


/**
 * Type: I/O school-command execution result
 *
 * @param <O> the type of command execution result value
 * @see oleg.sopilnyak.test.service.command.type.core.RootCommand#executeDo(Context)
 * @see Context#setResult(Object)
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public interface Output<O> extends IOBase<O> {
    // declared emptyValue output
    Output EMPTY = new EmptyResult<>();
    // The mapper for incoming values to module's model types
    BusinessMessagePayloadMapper payloadMapper = Mappers.getMapper(BusinessMessagePayloadMapper.class);

    /**
     * To get emptyResult value of parameter
     *
     * @return emptyResult parameter value
     */
    @Override
    default <T> IOBase<T> emptyValue() {
        return EMPTY;
    }

    /**
     * If a value is present, returns an {@code Output} describing
     * the result of applying the given mapping function to
     * the value, otherwise returns an emptyResult {@code Output}.
     *
     * <p>If the mapping function returns a {@code null} result then this method
     * returns an emptyResult {@code Output}.
     *
     * @param mapper the mapping function to apply to a value, if present
     * @return an {@code Output} describing the result of applying a mapping
     * function to the value of this {@code Output}, if a value is
     * present, otherwise an emptyResult {@code Output}
     * @throws NullPointerException if the mapping function is {@code null}
     * @apiNote This method supports post-processing on {@code Output} values, without
     * the need to explicitly check for a return status.  For example, the
     * following code traverses a stream of URIs, selects one that has not
     * yet been processed, and creates a path from that URI, returning
     * an {@code Output<Path>}:
     *
     * <pre>{@code
     *     Output<Path> p =
     *         uris.stream().filter(uri -> !isProcessedYet(uri))
     *                       .findFirst().orElseThrow()
     *                       .map(Output::of).map(Paths::get);
     * }</pre>
     * <p>
     * Here, {@code findFirst} returns an {@code Optional<URI>}, and then
     * {@code orElseThrow} returns value of {@code URI} which transformed to {@code Output<URI>}
     * then {@code map} returns an {@code Output<Path>} for the desired
     * URI if one exists.
     * @see Optional#map(Function)
     * @see Output#emptyResult()
     * @see Output#of
     */
    @Override
    default <U> Output<U> map(Function<? super O, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        if (isEmpty()) {
            return EMPTY;
        } else {
            return (Output<U>) Output.of(mapper.apply(value()));
        }
    }

    /**
     * To get emptyResult result output
     *
     * @return new instance of the output
     * @see EmptyResult
     */
    static <T> Output<T> emptyResult() {
        return EMPTY;
    }

    /**
     * To create new composite output instance (many outputs inside)
     *
     * @param outputs couple of outputs to join in composite
     * @param <T>     common type of output instance
     * @return new instance of the output
     * @see CompositeOutputParameter
     */
    static <T> CompositeOutput<T> of(Output<?>... outputs) {
        return new CompositeOutputParameter<>(outputs);
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
    static <T> Output<Optional<T>> of(final Optional<T> result) {
        return new OptionalValueResult<>(result);
    }


    /**
     * To create new output instance for DataMode base type (Payload)
     *
     * @param payload value of the payload
     * @param <T>     type of payload
     * @return new instance of the input
     * @see Input#mock(Object)
     * @see PayloadParameter
     * @see BasePayload
     * @see BaseType
     */
    static <T extends BasePayload<? extends BaseType>> Output<T> of(final T payload) {
        return MockUtil.isMock(payload) ? mock(payload) : new PayloadResult<>(payload);
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
    @SuppressWarnings("unchecked")
    static <P extends BasePayload<? extends BaseType>> Output<Set<P>> of(final Set<?> result) {
        if (CollectionUtils.isEmpty(result)) {
            return new PayloadSetResult<>(Set.of());
        }
        final Object item = result.iterator().next();
        if (item instanceof BasePayload<?>) {
            final Set<P> payloadsSet = result.stream().map(i -> (P) i).collect(Collectors.toSet());
            return new PayloadSetResult<>(payloadsSet);
        }
        return emptyResult();
    }

    /**
     * To create result output by result type (used in deserialization mostly)
     *
     * @param result instance to wrap
     * @return new instance of the output
     * @see Output#of(Boolean)
     * @see Output#of(BasePayload)
     * @see Output#of(Set)
     * @see Output#emptyResult()
     */
    static Output<?> of(final Object result) {
        return switch (result) {
            case null -> emptyResult();
            // check mocked objects
            case Object object when MockUtil.isMock(object) -> mock(object);
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

    /**
     * To build new output instance by parameter type
     *
     * @param type instance to wrap
     * @return new instance of the output
     */
    static <T extends BaseType> Output<?> of(final T type) {
        return switch (type) {
            case null -> emptyResult();
            case T base when MockUtil.isMock(base) -> mock(base);
            // education types
            case StudentPayload payload -> of(payload);
            case Student base -> of(payloadMapper.toPayload(base));
            case CoursePayload payload -> of(payload);
            case Course base -> of(payloadMapper.toPayload(base));
            // organization types
            case AuthorityPersonPayload payload -> of(payload);
            case AuthorityPerson base -> of(payloadMapper.toPayload(base));
            case FacultyPayload payload -> of(payload);
            case Faculty base -> of(payloadMapper.toPayload(base));
            case StudentsGroupPayload payload -> of(payload);
            case StudentsGroup base -> of(payloadMapper.toPayload(base));
            // profile types
            case PrincipalProfilePayload payload -> of(payload);
            case PrincipalProfile base -> of(payloadMapper.toPayload(base));
            case StudentProfilePayload payload -> of(payload);
            case StudentProfile base -> of(payloadMapper.toPayload(base));
            default -> throw new InvalidParameterTypeException("Model's BaseType", type);
        };
    }

    /**
     * To create new mocked output instance
     *
     * @param instance mocked value instance
     * @param <T>      type of the output
     * @return new instance of the output
     * @see Mocked
     */
    static <T> Output<T> mock(T instance) {
        return new Mocked<>(instance);
    }

    // inner class for mocked values

    /**
     * Output wrapper for mocked object
     *
     * @param value mocked value instance
     * @param <T>   type of the output
     */
    record Mocked<T>(T value) implements Output<T> {

        @Override
        public boolean equals(Object o) {
            return o instanceof Mocked(Object val) && value.equals(val);
        }
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
