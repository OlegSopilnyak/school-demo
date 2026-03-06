package oleg.sopilnyak.test.service.command.io;

import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.school.common.model.BaseType;
import oleg.sopilnyak.test.school.common.model.authentication.Role;
import oleg.sopilnyak.test.school.common.model.education.Course;
import oleg.sopilnyak.test.school.common.model.education.Student;
import oleg.sopilnyak.test.school.common.model.organization.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.organization.Faculty;
import oleg.sopilnyak.test.school.common.model.organization.StudentsGroup;
import oleg.sopilnyak.test.school.common.model.person.profile.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.person.profile.StudentProfile;
import oleg.sopilnyak.test.service.command.executable.core.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.parameter.CompositeInputParameter;
import oleg.sopilnyak.test.service.command.io.parameter.DequeContextsParameter;
import oleg.sopilnyak.test.service.command.io.parameter.EmptyParameter;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.io.parameter.NumberIdParameter;
import oleg.sopilnyak.test.service.command.io.parameter.PayloadParameter;
import oleg.sopilnyak.test.service.command.io.parameter.StaffRoleParameter;
import oleg.sopilnyak.test.service.command.io.parameter.StringParameter;
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
import java.util.Deque;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
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
 * @see oleg.sopilnyak.test.service.command.type.core.RootCommand#executeDo(Context)
 * @see CommandContext#setRedoParameter(Input)
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public interface Input<P> extends IOBase<P> {
    // declared emptyValue output
    Input EMPTY = new EmptyParameter();
    // The mapper for incoming values to module's model types
    BusinessMessagePayloadMapper payloadMapper = Mappers.getMapper(BusinessMessagePayloadMapper.class);

    /**
     * To get emptyResult value of parameter similar like Optional#emptyResult()
     *
     * @return emptyResult parameter value
     * @see Optional#empty()
     */
    @Override
    default <T> IOBase<T> emptyValue() {
        return EMPTY;
    }

    /**
     * If a value is present, returns an {@code Input} describing
     * the result of applying the given mapping function to
     * the value, otherwise returns an emptyResult {@code Input}.
     *
     * <p>If the mapping function returns a {@code null} result then this method
     * returns an emptyResult {@code Input}.
     *
     * @param mapper the mapping function to apply to a value, if present
     * @return an {@code Input} describing the result of applying a mapping
     * function to the value of this {@code Input}, if a value is
     * present, otherwise an emptyResult {@code Input}
     * @throws NullPointerException if the mapping function is {@code null}
     * @see Optional#map(Function)
     * @see Input#emptyParameter()
     * @see Input#of
     */
    @Override
    default <U> Input<U> map(Function<? super P, ? extends U> mapper) {
        Objects.requireNonNull(mapper);
        if (isEmpty()) {
            return EMPTY;
        } else {
            return (Input<U>) Input.of(mapper.apply(value()));
        }
    }

    /**
     * To create new composite input instance (many inputs inside)
     *
     * @param inputs couple of inputs to join in composite
     * @return new instance of the input
     * @param <T> common type of input instance
     */
    static <T> CompositeInput<T> of(Input<?>... inputs) {
        return new CompositeInputParameter<>(inputs);
    }

    /**
     * To create new emptyValue input instance
     *
     * @return new instance of the input
     * @see EmptyParameter
     */
    static <T> Input<T> emptyParameter() {
        return EMPTY;
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
     * To create new input long-type-pair-parameter instance
     *
     * @param firstId  first ID value
     * @param secondId second ID value
     * @return new instance of the input
     * @see Input#of(Input[])
     */
    static CompositeInput<Long> of(final Long firstId, final Long secondId) {
        return of(of(firstId), of(secondId));
    }

    /**
     * To create new input instance for String ID
     *
     * @param id ID value
     * @return new instance of the input
     * @see StringParameter
     */
    static Input<String> of(final String id) {
        return new StringParameter(id);
    }

    /**
     * To create new input string-type-pair-parameter instance
     *
     * @param firstValue  first value
     * @param secondValue second value
     * @return new instance of the input
     * @see Input#of(Input[])
     */
    static CompositeInput<String> of(final String firstValue, final String secondValue) {
        return of(of(firstValue), of(secondValue));
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
     * To create new input payload-type-pair-parameter instance
     *
     * @param firstPayload  first payload value
     * @param secondPayload second payload value
     * @return new instance of the input
     * @see CompositeInput
     * @see BasePayload
     * @see BaseType
     */
    static <T extends BasePayload<? extends BaseType>> CompositeInput<T> of(final T firstPayload, final T secondPayload) {
        return of(of(firstPayload), of(secondPayload));
    }

    /**
     * To create new input instance for Staff Role
     *
     * @param role value of the role
     * @return new instance of the input
     * @see Role
     * @see StaffRoleParameter
     */
    static Input<Role> of(final Role role) {
        return new StaffRoleParameter(role);
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
     * @see oleg.sopilnyak.test.service.command.executable.core.MacroCommand#executeDo(Context)
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
        return MockUtil.isMock(parameter) ? mock(parameter)
                : switch (parameter) {
            case null -> emptyParameter();
            case Input<?> input -> input;
            case Number numberId -> of(numberId);
            case String stringId -> of(stringId);
            default -> throw new IllegalArgumentException("Parameter type not supported: " + parameter.getClass());
        };
    }

    /**
     * To build new input instance by parameter type
     *
     * @param type instance to wrap
     * @return new instance of the input
     */
    static <T extends BaseType> Input<?> of(final T type) {
        return switch (type) {
            case null -> emptyParameter();
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
     * Input for mocked object
     *
     * @param value mocked value instance
     * @param <T>   type of the input
     */
    record MockedInput<T>(T value) implements Input<T> {

        @Override
        public boolean equals(Object o) {
            return o instanceof MockedInput(Object val) && value.equals(val);
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
        @SuppressWarnings("unchecked")
        public Input<I> deserialize(final JsonParser jsonParser, final DeserializationContext notUsed) throws IOException {
            final TreeNode parameterNode = jsonParser.readValueAsTree();
            final var inputParameterClass = IOBase.restoreIoBaseClass(parameterNode, Input.class);
            return ((ObjectMapper) jsonParser.getCodec()).readValue(parameterNode.toString(), inputParameterClass);
        }

    }
}
