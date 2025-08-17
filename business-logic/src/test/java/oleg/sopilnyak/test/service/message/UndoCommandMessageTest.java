package oleg.sopilnyak.test.service.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.core.CannotProcessActionException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.factory.farm.CommandsFactoriesFarm;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.NumberIdParameter;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {BusinessLogicConfiguration.class})
class UndoCommandMessageTest {
    private static final String COMMAND_ID = "command.id";
    private static final String CORRELATION_ID = "correlation-id";
    private static final String TEST_ACTION = "test-action";
    private static final String TEST_FACADE = "test-facade";
    @MockBean
    private PersistenceFacade persistenceFacade;
    @Autowired
    @Qualifier("jsonContextModule")
    private Module jsonContextModule;
    // json mapper
    private ObjectMapper objectMapper;
    @Autowired
    private CommandsFactoriesFarm farm;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .registerModule(jsonContextModule)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }


    @Test
    void shouldStoreAndRestoreFailedMessage_SimpleException() throws JsonProcessingException {
        long id = 1L;
        String commandId = "student.findById";
        String correlationId = "test-correlation-id";
        UndoCommandMessage<Optional<Student>> message = createMessage(correlationId, commandId, Input.of(id));
        message.getContext().setState(Context.State.WORK);
        String errorMessage = "Simple exception message";
        Exception ex = new Exception(errorMessage);
        message.getContext().failed(ex);

        String json = objectMapper.writeValueAsString(message);
        assertThat(json).isNotBlank();

        UndoCommandMessage<Boolean> restored = objectMapper.readValue(json, UndoCommandMessage.class);

        assertThat(restored).isInstanceOf(UndoCommandMessage.class);
        assertThat(restored.getCorrelationId()).isEqualTo(correlationId);
        assertThat(restored.getActionContext()).isNotNull();
        assertThat(restored.getActionContext().getActionName()).isNotNull().isEqualTo(TEST_ACTION);
        assertThat(restored.getActionContext().getFacadeName()).isNotNull().isEqualTo(TEST_FACADE);
        assertThat(restored.getContext()).isNotNull();
        assertThat(restored.getContext().getUndoParameter().isEmpty()).isFalse();
        assertThat(restored.getContext().getUndoParameter().value()).isEqualTo(id);
        assertThat(restored.getContext().getResult()).isNotNull().isEmpty();
        assertThat(restored.getContext().getException()).isNotNull().isInstanceOf(Exception.class);
        assertThat(restored.getContext().getException().getMessage()).isNotBlank().isEqualTo(errorMessage);
    }

    @Test
    void shouldStoreAndRestoreFailedMessage_CannotProcessActionException() throws JsonProcessingException {
        long id = 2L;
        String commandId = "student.findNotEnrolled";
        String correlationId = "test-correlation-id";
        UndoCommandMessage<Optional<Student>> message = createMessage(correlationId, commandId, Input.of(id));
        String errorMessage = "IO exception message";
        IOException ex = new IOException(errorMessage);
        ActionContext.install(message.getActionContext(), true);
        CannotProcessActionException exception = new CannotProcessActionException(ex);
        String masterExceptionMessage = exception.getMessage();
        int stackTraceDepth = ex.getStackTrace().length;
        message.getContext().failed(exception);

        String json = objectMapper.disable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(message);
        assertThat(json).contains(masterExceptionMessage.split("\\R|\\t"));
        UndoCommandMessage<Optional<Student>> restored = objectMapper.readValue(json, UndoCommandMessage.class);

        assertThat(restored).isInstanceOf(UndoCommandMessage.class);
        assertThat(restored.getContext().getUndoParameter().isEmpty()).isFalse();
        assertThat(restored.getContext().getUndoParameter().value()).isEqualTo(id);
        assertThat(restored.getContext().getException()).isNotNull().isInstanceOf(CannotProcessActionException.class);
        assertThat(restored.getContext().getException().getMessage()).isNotBlank().isEqualTo(masterExceptionMessage);
        assertThat(restored.getContext().getException().getStackTrace()).isNotNull().hasSize(stackTraceDepth);
        assertThat(restored.getContext().getException().getCause()).isNotNull().isInstanceOf(IOException.class);
        assertThat(restored.getContext().getException().getCause().getMessage()).isNotNull().isEqualTo(errorMessage);
        assertThat(restored.getContext().getException().getCause().getStackTrace()).isNotNull().hasSize(stackTraceDepth);
    }

    @Test
    void shouldRestoreUndoCommandMessageWithFail_SimpleException() throws IOException {
        long id = 3L;
        String commandId = "student.findById";
        String correlationId = "test-correlation-id";
        UndoCommandMessage<Optional<Student>> message = createMessage(correlationId, commandId, Input.of(id));
        message.getContext().setState(Context.State.WORK);
        String errorMessage = "Simple exception message";
        Exception ex = new Exception(errorMessage);
        int stackTraceDepth = ex.getStackTrace().length;
        message.getContext().failed(ex);

        String json = objectMapper.writeValueAsString(message);
        assertThat(json).isNotNull().isNotBlank().contains(errorMessage);

        UndoCommandMessage<Optional<Student>> restored = objectMapper.readValue(json, UndoCommandMessage.class);

        assertThat(restored).isNotNull().isInstanceOf(UndoCommandMessage.class);
        assertThat(restored.getContext().getUndoParameter().isEmpty()).isFalse();
        assertThat(restored.getContext().getUndoParameter().value()).isEqualTo(id);
        assertThat(restored.getContext().getException()).isNotNull().isInstanceOf(Exception.class);
        assertThat(restored.getContext().getException().getMessage()).isNotNull().isEqualTo(errorMessage);
        assertThat(restored.getContext().getException().getStackTrace()).isNotNull().hasSize(stackTraceDepth);
    }

    @Test
    void shouldCreateMessageLongBoolean() {
        long id = 4;
        Context.State state = Context.State.DONE;
        RootCommand<Boolean> command = mock(BooleanCommand.class);
        doReturn(COMMAND_ID).when(command).getId();
        CommandContext<Boolean> commandContext = CommandContext.<Boolean>builder().build();
        commandContext.setCommand(command);
        Instant startedAt = Instant.now();
        Duration duration = Duration.ofMillis(ChronoUnit.MILLIS.between(startedAt, Instant.now().plus(1, ChronoUnit.SECONDS)));

        // Create a UndoCommandMessage instance
        UndoCommandMessage<Boolean> message = UndoCommandMessage.<Boolean>builder().build();
        message.setCorrelationId(CORRELATION_ID);

        // Set the command ID, action context and command context
        ActionContext actionContext = ActionContext.builder().actionName(TEST_ACTION).facadeName(TEST_FACADE).build();
        message.setContext(commandContext);
        message.setActionContext(actionContext);

        // Set the undo parameter
        commandContext.setState(Context.State.WORK);
        commandContext.setUndoParameter(new NumberIdParameter<>(id));
        commandContext.setState(state);
        commandContext.setStartedAt(startedAt);
        commandContext.setDuration(duration);

        assertThat(message.getCorrelationId()).isSameAs(CORRELATION_ID);
        assertThat(message.getContext().getCommand().getId()).isSameAs(COMMAND_ID);
        assertThat(message.getActionContext()).isSameAs(actionContext);
        assertThat(message.getContext().getUndoParameter().isEmpty()).isFalse();
        assertThat(message.getContext().getUndoParameter().value()).isSameAs(id);
        assertThat(message.getContext().getState()).isSameAs(state);
        assertThat(message.getContext().getStartedAt()).isSameAs(startedAt);
        assertThat(message.getContext().getDuration()).isSameAs(duration);
    }

    @Test
    void shouldStoreCommandMessageLongBoolean() throws JsonProcessingException {
        long id = 5;
        Context.State state = Context.State.DONE;
        RootCommand<Boolean> command = mock(BooleanCommand.class);
        doReturn(COMMAND_ID).when(command).getId();
        CommandContext<Boolean> commandContext = CommandContext.<Boolean>builder().build();
        commandContext.setCommand(command);
        ActionContext actionContext = ActionContext.builder().actionName(TEST_ACTION).facadeName(TEST_FACADE).build();
        Instant startedAt = Instant.now();
        Duration duration = Duration.ofMillis(ChronoUnit.MILLIS.between(startedAt, Instant.now().plus(100, ChronoUnit.MILLIS)));
        // Create a UndoCommandMessage instance
        UndoCommandMessage<Boolean> message = UndoCommandMessage.<Boolean>builder().build();
        message.setCorrelationId(CORRELATION_ID);

        // Set the command ID, action context and command context
        message.setContext(commandContext);
        message.setActionContext(actionContext);

        // Set the undo parameter
        commandContext.setState(Context.State.WORK);
        commandContext.setUndoParameter(new NumberIdParameter<>(id));
        commandContext.setState(state);
        commandContext.setStartedAt(startedAt);
        commandContext.setDuration(duration);

        String json = objectMapper.writeValueAsString(message);

        assertThat(json).isNotBlank()
                .contains(TEST_FACADE).contains(TEST_ACTION)
                .contains(NumberIdParameter.class.getName());

    }

    @Test
    void shouldRestoreCommandMessageLongBoolean() throws JsonProcessingException {
        String commandId = "student.findById";
        long id = 6;
        Context.State state = Context.State.DONE;
        CommandContext<Boolean> commandContext = CommandContext.<Boolean>builder().command(farm.command(commandId)).build();
        ActionContext actionContext = ActionContext.builder().actionName("test-action").facadeName("test-facade").build();
        Instant startedAt = Instant.now();
        Duration duration = Duration.ofMillis(ChronoUnit.MILLIS.between(startedAt, Instant.now().plus(100, ChronoUnit.MILLIS)));

        // Create a UndoCommandMessage instance
        UndoCommandMessage<Boolean> message = UndoCommandMessage.<Boolean>builder().build();
        message.setCorrelationId(CORRELATION_ID);

        // Set the command ID, action context and command context
        message.setContext(commandContext);
        message.setActionContext(actionContext);

        // Set the undo parameter
        commandContext.setState(Context.State.WORK);
        commandContext.setUndoParameter(new NumberIdParameter<>(id));
        commandContext.setState(state);
        commandContext.setStartedAt(startedAt);
        commandContext.setDuration(duration);

        // Serialize the message to JSON
        String json = objectMapper.writeValueAsString(message);

        UndoCommandMessage<Boolean> restoredMessage = objectMapper.readValue(json, UndoCommandMessage.class);

        assertThat(restoredMessage).isNotNull().isEqualTo(message);
    }


    // inner types
    interface BooleanCommand extends RootCommand<Boolean> {
        // This interface is just a marker for commands that return Boolean
    }
    // private methods
    private <T> UndoCommandMessage<T> createMessage(String correlationId, String commandId, Input<?> input) {
        CommandContext<T> context = CommandContext.<T>builder()
                .command(farm.command(commandId)).undoParameter(input)
                .startedAt(Instant.now()).duration(Duration.of(10, ChronoUnit.SECONDS))
                .build();
        context.setState(Context.State.INIT);
        context.setState(Context.State.READY);
        return UndoCommandMessage.<T>builder()
                .correlationId(correlationId)
                .context(context)
                .actionContext(ActionContext.builder().actionName(TEST_ACTION).facadeName(TEST_FACADE).build())
                .build();
    }
}
