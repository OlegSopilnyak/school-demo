package oleg.sopilnyak.test.service.command.io;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.parameter.DequeContextsParameter;
import oleg.sopilnyak.test.service.command.io.parameter.EmptyParameter;
import oleg.sopilnyak.test.service.command.io.parameter.LongIdPairParameter;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.io.parameter.NumberIdParameter;
import oleg.sopilnyak.test.service.command.io.parameter.PairParameter;
import oleg.sopilnyak.test.service.command.io.parameter.PayloadPairParameter;
import oleg.sopilnyak.test.service.command.io.parameter.PayloadParameter;
import oleg.sopilnyak.test.service.command.io.parameter.StringIdParameter;
import oleg.sopilnyak.test.service.command.io.parameter.StringPairParameter;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.CommandMessage;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;
import oleg.sopilnyak.test.service.message.payload.BasePayload;
import oleg.sopilnyak.test.service.message.payload.BaseProfilePayload;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;
import oleg.sopilnyak.test.service.message.payload.FacultyPayload;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
import oleg.sopilnyak.test.service.message.payload.StudentProfilePayload;
import oleg.sopilnyak.test.service.message.payload.StudentsGroupPayload;

import java.io.IOException;
import java.util.Deque;
import org.mockito.internal.util.MockUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.mapstruct.factory.Mappers;

/**
 * Type: I/O school-command input parameter
 *
 * @param <P> the type of command input parameter
 * @see oleg.sopilnyak.test.service.command.type.base.RootCommand#executeDo(Context)
 * @see CommandContext#setRedoParameter(Input)
 */
public interface Input<P> extends IOBase<P> {

    BusinessMessagePayloadMapper payloadMapper = Mappers.getMapper(BusinessMessagePayloadMapper.class);

    /**
     * To create new empty input instance
     *
     * @return new instance of the input
     * @see EmptyParameter
     */
    static Input<Void> empty() {
        return new EmptyParameter();
    }

    /**
     * To create new mocked input instance
     *
     * @param instance mocked value instance
     * @param <T>      type of the input
     * @return new instance of the input
     * @see MockedInput
     */
    static <T> Input<T> mock(T instance) {
        return new MockedInput<>(instance);
    }

    /**
     * To create new input instance for Number ID
     *
     * @param id ID value
     * @return new instance of the input
     * @see NumberIdParameter
     */
    static <T extends Number> Input<T> of(final T id) {
        return new NumberIdParameter<>(id);
    }

    /**
     * To create new input long-pair-parameter instance
     *
     * @param firstId  first ID value
     * @param secondId second ID value
     * @return new instance of the input
     * @see LongIdPairParameter
     * @see PairParameter
     */
    static PairParameter<Long> of(final Long firstId, final Long secondId) {
        return new LongIdPairParameter(firstId, secondId);
    }

    /**
     * To create new input instance for String ID
     *
     * @param id ID value
     * @return new instance of the input
     * @see StringIdParameter
     */
    static Input<String> of(final String id) {
        return new StringIdParameter(id);
    }

    /**
     * To create new input string-pair-parameter instance
     *
     * @param firstValue  first value
     * @param secondValue second value
     * @return new instance of the input
     * @see StringPairParameter
     * @see PairParameter
     */
    static PairParameter<String> of(final String firstValue, final String secondValue) {
        return new StringPairParameter(firstValue, secondValue);
    }

    /**
     * To create new input instance for Payload
     *
     * @param payload value of the payload
     * @param <T>     type of payload
     * @return new instance of the input
     * @see Input#mock(Object)
     * @see PayloadParameter
     * @see BasePayload
     * @see BaseType
     */
    static <T extends BasePayload<? extends BaseType>> Input<T> of(final T payload) {
        return MockUtil.isMock(payload) ? mock(payload) : new PayloadParameter<>(payload);
    }

    /**
     * To create new input payload-pair-parameter instance
     *
     * @param firstPayload  first payload value
     * @param secondPayload second payload value
     * @return new instance of the input
     * @see PayloadPairParameter
     * @see PairParameter
     * @see BasePayload
     * @see BaseType
     */
    static <T extends BasePayload<? extends BaseType>> PairParameter<T> of(final T firstPayload, final T secondPayload) {
        return new PayloadPairParameter<>(firstPayload, secondPayload);
    }

    /**
     * To create new input for MacroCommand
     *
     * @param rootInput root command's input parameter instance
     * @param contexts  contexts built for root input parameter from nested commands
     * @return new instance of the input
     * @see Input
     * @see MacroCommandParameter
     */
    static Input<MacroCommandParameter> of(final Input<?> rootInput, final Deque<Context<?>> contexts) {
        return new MacroCommandParameter(rootInput, contexts);
    }

    /**
     * To create new input contexts-deque-parameter instance<BR/>
     * Used for undo command sequence in CompositeCommand
     *
     * @param contexts sequence context for undo processing in CompositeCommand
     * @return new instance of the input
     * @see DequeContextsParameter
     * @see Deque
     * @see Context
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#executeDo(Context)
     */
    static Input<Deque<Context<?>>> of(final Deque<Context<?>> contexts) {
        return new DequeContextsParameter(contexts);
    }

    /**
     * To create new input by parameter type
     *
     * @param parameter instance to wrap
     * @return new instance of the input
     */
    static Input<?> of(final Object parameter) {
        if (isNull(parameter)) return empty();
        if (MockUtil.isMock(parameter)) return mock(parameter);
        else if (parameter instanceof Input<?> input) return input;
        else if (parameter instanceof Number numberId) return of(numberId);
        else if (parameter instanceof String stringId) return of(stringId);

        throw new IllegalArgumentException("Parameter type not supported: " + parameter.getClass());
    }


    static <T extends BaseType> Input<?> of(final T type) {
        if (MockUtil.isMock(type)) return mock(type);

        final var educationInput = educationType(type);
        if (nonNull(educationInput)) return educationInput;

        final var organizationInput = organizationType(type);
        if (nonNull(organizationInput)) return organizationInput;

        final var profileInput = profileType(type);
        if (nonNull(profileInput)) return profileInput;

        throw new IllegalArgumentException("Parameter type not supported: " + type);
    }

    /**
     * Input for mocked object
     *
     * @param value mocked value instance
     * @param <T>   type of the input
     */
    record MockedInput<T>(T value) implements Input<T> {

        @Override
        public boolean equals(Object o) {
            return o instanceof MockedInput<?> that && value.equals(that.value);
        }

    }

    // inner classes for JSON serializing/deserializing

    /**
     * JSON: Deserializer for Input Parameter field of the command-message
     *
     * @see StdDeserializer
     * @see CommandMessage#getContext()
     * @see Context#getRedoParameter()
     * @see Context#getUndoParameter()
     * @see Input
     */
    class ParameterDeserializer<I> extends StdDeserializer<Input<?>> {

        public ParameterDeserializer() {
            this(Input.class);
        }

        protected ParameterDeserializer(Class<? extends Input> vc) {
            super(vc);
        }

        @Override
        public Input<I> deserialize(final JsonParser jsonParser,
                                    final DeserializationContext deserializationContext) throws IOException {
            final TreeNode parameterNode = jsonParser.readValueAsTree();
            final ObjectMapper mapper = (ObjectMapper) jsonParser.getCodec();
            final Class<? extends Input> inputParameterClass = IOBase.restoreIoBaseClass(parameterNode, Input.class);
            return mapper.readValue(parameterNode.toString(), inputParameterClass);
        }

    }

    // private methods
    private static <T extends BaseType> Input<? extends BaseProfilePayload<? extends PersonProfile>> profileType(T type) {
        if (type instanceof PrincipalProfile base)
            return type instanceof PrincipalProfilePayload payload ? of(payload) : of(payloadMapper.toPayload(base));
        if (type instanceof StudentProfile base)
            return type instanceof StudentProfilePayload payload ? of(payload) : of(payloadMapper.toPayload(base));
        return null;
    }

    private static <T extends BaseType> Input<? extends BasePayload<? extends BaseType>> organizationType(T type) {
        if (type instanceof AuthorityPerson base)
            return type instanceof AuthorityPersonPayload payload ? of(payload) : of(payloadMapper.toPayload(base));
        if (type instanceof Faculty base)
            return type instanceof FacultyPayload payload ? of(payload) : of(payloadMapper.toPayload(base));
        if (type instanceof StudentsGroup base)
            return type instanceof StudentsGroupPayload payload ? of(payload) : of(payloadMapper.toPayload(base));
        return null;
    }

    private static <T extends BaseType> Input<? extends BasePayload<? extends BaseType>> educationType(T type) {
        return switch (type) {
            case Student base ->
                    type instanceof StudentPayload payload ? of(payload) : of(payloadMapper.toPayload(base));
            case Course base -> type instanceof CoursePayload payload ? of(payload) : of(payloadMapper.toPayload(base));
            case null, default -> null;
        };
    }

}
