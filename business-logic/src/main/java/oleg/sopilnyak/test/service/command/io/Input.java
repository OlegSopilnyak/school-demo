package oleg.sopilnyak.test.service.command.io;

import oleg.sopilnyak.test.school.common.model.*;
import oleg.sopilnyak.test.service.command.io.parameter.*;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.*;
import org.mapstruct.factory.Mappers;
import org.mockito.internal.util.MockUtil;

import java.util.Deque;

import static java.util.Objects.nonNull;

/**
 * Type: I/O school-command input parameter
 *
 * @param <P> the type of command input parameter
 * @see oleg.sopilnyak.test.service.command.type.base.RootCommand#executeDo(Context)
 * @see oleg.sopilnyak.test.service.command.executable.sys.CommandContext#setRedoParameter(Input)
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
     * To create new input contexts-deque-parameter instance<BR/>
     * Used for undo command sequence in CompositeCommand
     *
     * @param contexts sequence context for undo action in CompositeCommand
     * @return new instance of the input
     * @see UndoDequeContextsParameter
     * @see Deque
     * @see Context
     * @see oleg.sopilnyak.test.service.command.executable.sys.MacroCommand#executeDo(Context)
     */
    static Input<Deque<Context<?>>> of(final Deque<Context<?>> contexts) {
        return new UndoDequeContextsParameter(contexts);
    }

    static Input<?> of(final Object parameter) {
        if (MockUtil.isMock(parameter)) return mock(parameter);
        else if (parameter instanceof Input<?> input) return input;
        else if (parameter instanceof Number numberId) return of(numberId);
        else if (parameter instanceof String stringId) return of(stringId);

        throw new IllegalArgumentException("Parameter type not supported: " + parameter.getClass());
    }


    static <T extends BaseType> Input<?> of(final T type) {
        if (MockUtil.isMock(type)) return mock(type);

        final Input<? extends BasePayload<? extends BaseType>> educationInput = educationType(type);
        if (nonNull(educationInput)) return educationInput;

        final Input<? extends BasePayload<? extends BaseType>> organizationInput = organizationType(type);
        if (nonNull(organizationInput)) return organizationInput;

        final Input<? extends BaseProfilePayload<? extends PersonProfile>> profileInput = profileType(type);
        if (nonNull(profileInput)) return profileInput;

        throw new IllegalArgumentException("Parameter type not supported: " + type);
    }

    // private classes
    record MockedInput<T>(T value) implements Input<T> {

        @Override
        public boolean equals(Object o) {
            return o instanceof MockedInput<?> that &&  value.equals(that.value);
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
        if (type instanceof Student base)
            return type instanceof StudentPayload payload ? of(payload) : of(payloadMapper.toPayload(base));
        else if (type instanceof Course base)
            return type instanceof CoursePayload payload ? of(payload) : of(payloadMapper.toPayload(base));
        else return null;
    }

}
