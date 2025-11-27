package oleg.sopilnyak.test.service.command.io.parameter;

import static java.util.Objects.isNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.factory.farm.CommandsFactoriesFarm;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.JsonContextModule;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SchoolCommandsConfiguration.class)
class ContextInputParameterTest {
    private static final String STUDENT_FIND_BY_ID = "student.findById";
    private static final String STUDENT_FIND_ENROLLED = "student.findEnrolledTo";
    private static final String COURSE_FIND_BY_ID = "course.findById";
    private static final String COURSE_FIND_REGISTERED = "course.findRegisteredFor";
    @MockBean
    private PersistenceFacade persistenceFacade;
    @MockBean
    private BusinessMessagePayloadMapper mapper;

    @SpyBean
    @Autowired
    private CommandsFactoriesFarm<? extends RootCommand<?>> farm;
    @Autowired
    @Qualifier("jsonContextModule")
    private Module jsonContextModule;
    // json mapper
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(jsonContextModule)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    void scheckTestIntegrity() {
        assertThat(farm).isNotNull();
        assertThat(jsonContextModule).isNotNull().isInstanceOf(JsonContextModule.class);
        assertThat(ReflectionTestUtils.getField(jsonContextModule, "farm")).isSameAs(farm);
    }

    @Test
    void shouldCreateUndoDequeContextsParameter() {
        Context<?> context1 = mock(Context.class);
        Context<?> context2 = mock(Context.class);
        Deque<Context<?>> contexts = Stream.of(context1, context2).collect(Collectors.toCollection(LinkedList::new));

        Input<Deque<Context<?>>> parameter = Input.of(contexts);

        assertThat(parameter).isInstanceOf(Input.class).isInstanceOf(DequeContextsParameter.class);
        assertThat(parameter.value()).hasSameSizeAs(contexts);
        assertThat(parameter.value()).contains(context1).contains(context2);
    }

    @Test
    void shouldRestoreUndoDequeContextsParameter() throws JsonProcessingException {
        String commandId = STUDENT_FIND_BY_ID;
        RootCommand<?> command = farm.command(commandId);
        // context 1
        Context<Boolean> context1 = (Context<Boolean>) command.createContext(Input.of(1));
        setUndoParameter(context1, Input.of(-1L));
        context1.setState(Context.State.WORK);
        context1.setResult(true);
        context1.setState(Context.State.DONE);
        context1.setState(Context.State.WORK);
        context1.failed((Exception) new UnableExecuteCommandException(commandId).fillInStackTrace());

        commandId = STUDENT_FIND_ENROLLED;
        command = farm.command(commandId);
        // context 2
        Context<Student> context2 = (Context<Student>) command.createContext(Input.of(createStudent(10L)));
        setUndoParameter(context2, Input.of(-2L));
        context2.setState(Context.State.WORK);
        context2.setResult(createStudent(1L));
        context2.setState(Context.State.UNDONE);
        context2.setState(Context.State.WORK);
        context2.failed((Exception) new UnableExecuteCommandException(commandId).fillInStackTrace());

        Deque<Context<?>> contexts = new LinkedList<>(List.of(context1, context2));
        Input<Deque<Context<?>>> parameter = Input.of(contexts);

        String json = objectMapper.writeValueAsString(parameter);

        assertThat(json)
                .contains(DequeContextsParameter.class.getName())
                .contains(UnableExecuteCommandException.class.getName())
                .contains(commandId);

        DequeContextsParameter restored = objectMapper.readValue(json, DequeContextsParameter.class);

        assertThat(restored).isInstanceOf(DequeContextsParameter.class);
        assertThat(restored.value()).hasSameSizeAs(contexts);
        Deque<Context<?>> restoredContexts = new LinkedList<>(restored.value());
        assertEquals(context1, restoredContexts.pop());
        assertEquals(context2, restoredContexts.pop());
    }

    @Test
    void shouldCreateMacroCommandInputParameter() {
        Input<Long> rootInput = Input.of(1L);
        Context<?> context1 = mock(Context.class);
        Context<?> context2 = mock(Context.class);
        Deque<Context<?>> contexts = Stream.of(context1, context2).collect(Collectors.toCollection(LinkedList::new));

        Input<MacroCommandParameter> parameter = Input.of(rootInput, contexts);

        assertThat(parameter).isNotNull().isInstanceOf(Input.class).isInstanceOf(MacroCommandParameter.class);
        assertThat(parameter.value().getNestedContexts()).hasSameSizeAs(contexts);
        assertThat(parameter.value().getNestedContexts()).contains(context1).contains(context2);
    }

    @Test
    void shouldRestoreMacroCommandInputParameter() throws JsonProcessingException {
        Input<StudentPayload> rootInput = Input.of(createStudent(111L));
        String commandId = COURSE_FIND_BY_ID;
        RootCommand<?> command = farm.command(commandId);
        // context 1
        Context<Boolean> context1 = (Context<Boolean>) command.createContext(Input.of(1));
        setUndoParameter(context1, Input.of(-1L));
        context1.setState(Context.State.WORK);
        context1.setResult(true);
        context1.setState(Context.State.DONE);
        context1.setState(Context.State.WORK);
        context1.failed((Exception) new UnableExecuteCommandException(commandId).fillInStackTrace());

        commandId = COURSE_FIND_REGISTERED;
        command = farm.command(commandId);
        // context 2
        Context<Student> context2 = (Context<Student>) command.createContext(Input.of(createStudent(11L)));
        setUndoParameter(context2, Input.of(-2L));
        context2.setState(Context.State.WORK);
        context2.setResult(createStudent(2L));
        context2.setState(Context.State.UNDONE);
        context2.setState(Context.State.WORK);
        context2.failed((Exception) new UnableExecuteCommandException(commandId).fillInStackTrace());

        Deque<Context<?>> contexts = new LinkedList<>(List.of(context1, context2));

        Input<MacroCommandParameter> parameter = Input.of(rootInput, contexts);

        String json = objectMapper.writeValueAsString(parameter);

        assertThat(json)
                .contains(MacroCommandParameter.class.getName())
                .contains(UnableExecuteCommandException.class.getName())
                .contains(commandId);

        MacroCommandParameter restored = objectMapper.readValue(json, MacroCommandParameter.class);

        assertThat(restored).isInstanceOf(Input.class).isInstanceOf(MacroCommandParameter.class);
        assertThat(restored.getRootInput()).isEqualTo(rootInput);
        assertThat(restored.getNestedContexts()).hasSameSizeAs(contexts);
        Deque<Context<?>> restoredContexts = new LinkedList<>(restored.getNestedContexts());
        assertEquals(context1, restoredContexts.pop());
        assertEquals(context2, restoredContexts.pop());
    }

    // private methods
    private static void assertEquals(Context<?> expected, Context<?> actual) {
        // command
        assertThat(expected.getCommand()).isSameAs(actual.getCommand());
        // started at
        if (isNull(expected.getStartedAt())) {
            assertThat(actual.getStartedAt()).isNull();
        } else {
            assertThat(expected.getStartedAt()).isEqualTo(actual.getStartedAt());
        }
        // execution duration
        if (isNull(expected.getDuration())) {
            assertThat(actual.getDuration()).isNull();
        } else {
            assertThat(expected.getDuration()).isEqualTo(actual.getDuration());
        }
        // execution state
        assertThat(expected.getState()).isSameAs(actual.getState());
        // redo input parameter
        if (isNull(expected.getRedoParameter())) {
            assertThat(actual.getRedoParameter()).isNull();
        } else if (expected.getRedoParameter().isEmpty()) {
            assertThat(actual.getRedoParameter().isEmpty()).isTrue();
        } else {
            assertThat(expected.getRedoParameter().value()).isEqualTo(actual.getRedoParameter().value());
        }
        // undo input parameter
        if (isNull(expected.getUndoParameter())) {
            assertThat(actual.getUndoParameter()).isNull();
        } else if (expected.getUndoParameter().isEmpty()) {
            assertThat(actual.getUndoParameter().isEmpty()).isTrue();
        } else {
            assertThat(expected.getUndoParameter().value()).isEqualTo(actual.getUndoParameter().value());
        }
        // result
        if (isNull(expected.getResult())) {
            assertThat(actual.getResult()).isNull();
        } else if (expected.getResult().isEmpty()) {
            assertThat(actual.getResult()).isEmpty();
        } else {
            assertThat(actual.getResult().get()).isEqualTo(expected.getResult().get());
        }
        // error
        if (isNull(expected.getException())) {
            assertThat(actual.getException()).isNull();
        } else {
            assertEquals(expected.getException(), actual.getException());
        }
        // history
        if (isNull(expected.getHistory())) {
            assertThat(actual.getHistory()).isNull();
        } else {
            assertThat(expected.getHistory()).isEqualTo(actual.getHistory());
        }
    }

    private static void assertEquals(Exception expected, Exception actual) {
        assertThat(expected.getMessage()).isEqualTo(actual.getMessage());
        assertThat(expected.getCause()).isEqualTo(actual.getCause());
        assertThat(expected.getStackTrace()).hasSameSizeAs(actual.getStackTrace());
    }

    private static StudentPayload createStudent(long id) {
        return StudentPayload.builder()
                .id(id)
                .originalType("not-a-student-" + id)
                .profileId(id + 1)
                .firstName("John-" + id)
                .lastName("Smith-" + id)
                .gender("Male")
                .description("student-description-" + id)
                .courses(List.of())
                .build();
    }

    private void setUndoParameter(Context<?> context, Input<?> parameter) {
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setState(Context.State.WORK);
            commandContext.setUndoParameter(parameter);
        } else {
            fail("Wrong type of context");
        }
    }
}
