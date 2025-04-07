package oleg.sopilnyak.test.service.command.io.parameter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.configurations.SchoolCommandsConfiguration;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.factory.farm.CommandsFactoriesFarm;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.JsonContextModule;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = SchoolCommandsConfiguration.class)
public class ContextInputParameterTest {
    @MockBean
    private PersistenceFacade persistenceFacade;
    @MockBean
    private BusinessMessagePayloadMapper mapper;

    @SpyBean
    @Autowired
    private CommandsFactoriesFarm farm;
    @Autowired
    @Qualifier("jsonContextModule")
    private Module jsonContextModule;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(jsonContextModule)
                .enable(SerializationFeature.INDENT_OUTPUT);
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

        assertThat(parameter.value()).hasSameSizeAs(contexts);
        assertThat(parameter.value()).contains(context1).contains(context2);
        assertThat(parameter).isInstanceOf(DequeContextsParameter.class);
    }

    @Test
    void shouldRestoreUndoDequeContextsParameter() throws JsonProcessingException {
        String commandId = "test-student-command";
        StudentCommand command = new StudentCommand() {
            @Override
            public Logger getLog() {
                return LoggerFactory.getLogger(getClass());
            }

            @Override
            public String getId() {
                return commandId;
            }
        };
        doReturn(command).when(farm).command(commandId);
        Exception error = new UnableExecuteCommandException(command.getId());
        error.fillInStackTrace();
        Context<Boolean> context1 = command.createContext(Input.of(1));
        setUndoParameter(context1, Input.of(-1L));
        context1.setState(Context.State.WORK);
        context1.setResult(true);
        context1.setState(Context.State.DONE);
        context1.setState(Context.State.WORK);
        context1.failed((Exception) new UnableExecuteCommandException(command.getId()).fillInStackTrace());

        Student result = createStudent(1L);
        Context<Student> context2 = command.createContext(Input.of(2));
        setUndoParameter(context2, Input.of(-2L));
        context2.setState(Context.State.WORK);
        context2.setResult(result);
        context2.setState(Context.State.UNDONE);
        context2.setState(Context.State.WORK);
        context2.failed((Exception) new UnableExecuteCommandException(command.getId()).fillInStackTrace());

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
    }

    // private methods
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
