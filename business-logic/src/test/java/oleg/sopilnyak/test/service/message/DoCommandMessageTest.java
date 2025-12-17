package oleg.sopilnyak.test.service.message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.core.CannotProcessActionException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.factory.farm.CommandsFactoriesFarm;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.NumberIdParameter;
import oleg.sopilnyak.test.service.command.io.result.BooleanResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.configuration.BusinessLogicConfiguration;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {BusinessLogicConfiguration.class})
class DoCommandMessageTest {
    private static final String COMMAND_ID = "command.id";
    private static final String CORRELATION_ID = "correlation-id";
    private static final String TEST_ACTION = "test-processing";
    private static final String TEST_FACADE = "test-facade";
    @MockitoBean
    private PersistenceFacade persistenceFacade;
    // json mapper
    @Autowired
    @Qualifier("commandsTroughMessageObjectMapper")
    private ObjectMapper objectMapper;
    @Autowired
    private CommandsFactoriesFarm farm;

    @BeforeEach
    void setUp() {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Test
    void shouldStoreAndRestoreFailedMessage_SimpleException() throws JsonProcessingException {
        String commandId = "student.findById";
        String correlationId = "test-correlation-id";
        DoCommandMessage<Optional<Student>> message = createMessage(correlationId, commandId, Input.of(1L));
        message.getContext().setState(Context.State.WORK);
        ((CommandContext) message.getContext()).setResultData(true);
        String errorMessage = "Simple exception message";
        Exception ex = new Exception(errorMessage);
        message.getContext().failed(ex);

        String json = objectMapper.writeValueAsString(message);
        assertThat(json).isNotBlank();

        DoCommandMessage<Boolean> restored = objectMapper.readValue(json, DoCommandMessage.class);

        assertThat(restored).isInstanceOf(DoCommandMessage.class);
        assertThat(restored.getCorrelationId()).isEqualTo(correlationId);
        assertThat(restored.getActionContext()).isNotNull();
        assertThat(restored.getActionContext().getActionName()).isNotNull().isEqualTo(TEST_ACTION);
        assertThat(restored.getActionContext().getFacadeName()).isNotNull().isEqualTo(TEST_FACADE);
        assertThat(restored.getContext()).isNotNull();
        assertThat(restored.getContext().getRedoParameter().isEmpty()).isFalse();
        assertThat(restored.getContext().getRedoParameter().value()).isEqualTo(1L);
        assertThat(restored.getContext().getResult()).isEmpty();
        assertThat(restored.getContext().getException()).isNotNull().isInstanceOf(Exception.class);
        assertThat(restored.getContext().getException().getMessage()).isNotBlank().isEqualTo(errorMessage);
    }

    @Test
    void shouldStoreAndRestoreFailedMessage_CannotProcessActionException() throws JsonProcessingException {
        String commandId = "student.findNotEnrolled";
        String correlationId = "test-correlation-id";
        DoCommandMessage<Optional<Student>> message = createMessage(correlationId, commandId, Input.of(2L));
        String errorMessage = "IO exception message";
        IOException ex = new IOException(errorMessage);
        ActionContext.install(message.getActionContext(), true);
        CannotProcessActionException exception = new CannotProcessActionException(ex);
        String masterExceptionMessage = exception.getMessage();
        int stackTraceDepth = ex.getStackTrace().length;
        message.getContext().failed(exception);

        String json = objectMapper.disable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(message);
        assertThat(json).contains(masterExceptionMessage.split("\\R|\\t"));
        DoCommandMessage<Optional<Student>> restored = objectMapper.readValue(json, DoCommandMessage.class);

        assertThat(restored).isInstanceOf(DoCommandMessage.class);
        assertThat(restored.getContext().getException()).isNotNull().isInstanceOf(CannotProcessActionException.class);
        assertThat(restored.getContext().getException().getMessage()).isNotBlank().isEqualTo(masterExceptionMessage);
        assertThat(restored.getContext().getException().getStackTrace()).isNotNull().hasSize(stackTraceDepth);
        assertThat(restored.getContext().getException().getCause()).isNotNull().isInstanceOf(IOException.class);
        assertThat(restored.getContext().getException().getCause().getMessage()).isNotNull().isEqualTo(errorMessage);
        assertThat(restored.getContext().getException().getCause().getStackTrace()).isNotNull().hasSize(stackTraceDepth);
    }

    @Test
    void shouldRestoreDoCommandMessageWithFail_SimpleException() throws IOException {
        String commandId = "student.findById";
        String correlationId = "test-correlation-id";
        DoCommandMessage<Optional<Student>> message = createMessage(correlationId, commandId, Input.of(3L));
        message.getContext().setState(Context.State.WORK);
        ((CommandContext) message.getContext()).setResult(true);
        String errorMessage = "Simple exception message";
        Exception ex = new Exception(errorMessage);
        int stackTraceDepth = ex.getStackTrace().length;
        message.getContext().failed(ex);

        String json = objectMapper.writeValueAsString(message);
        assertThat(json).isNotNull().isNotBlank().contains(errorMessage);

        DoCommandMessage<Optional<Student>> restored = objectMapper.readValue(json, DoCommandMessage.class);

        assertThat(restored).isNotNull().isInstanceOf(DoCommandMessage.class);
        assertThat(restored.getContext().getException()).isNotNull().isInstanceOf(Exception.class);
        assertThat(restored.getContext().getException().getMessage()).isNotNull().isEqualTo(errorMessage);
        assertThat(restored.getContext().getException().getStackTrace()).isNotNull().hasSize(stackTraceDepth);
    }

    @Test
    void shouldCreateMessageLongBoolean() {
        String commandId = DoCommandMessageTest.COMMAND_ID;
        String correlationCommandId = DoCommandMessageTest.CORRELATION_ID;
        long id = 1;
        boolean value = true;
        Context.State state = Context.State.DONE;
        RootCommand<Boolean> command = mock(BooleanCommand.class);
        doReturn(commandId).when(command).getId();
        CommandContext<Boolean> commandContext = CommandContext.<Boolean>builder().build();
        commandContext.setCommand(command);
        Instant startedAt = Instant.now();
        Duration duration = Duration.ofMillis(ChronoUnit.MILLIS.between(startedAt, Instant.now().plus(1, ChronoUnit.SECONDS)));
        ActionContext actionContext = ActionContext.builder().actionName(TEST_ACTION).facadeName(TEST_FACADE).build();

        // Create a DoCommandMessage instance
        // Set the command ID, processing context and command context
        DoCommandMessage<Boolean> message = DoCommandMessage.<Boolean>builder()
                .correlationId(CORRELATION_ID).actionContext(actionContext).context(commandContext)
                .build();

        // Set the parameter and result
        commandContext.setRedoParameter(new NumberIdParameter<>(id));
        commandContext.setState(Context.State.WORK);
        commandContext.setResult(value);
        commandContext.setState(state);
        commandContext.setStartedAt(startedAt);
        commandContext.setDuration(duration);

        assertThat(message.getCorrelationId()).isSameAs(correlationCommandId);
        assertThat(message.getContext().getCommand().getId()).isSameAs(commandId);
        assertThat(message.getActionContext()).isSameAs(actionContext);
        assertThat(message.getContext().getRedoParameter().value()).isSameAs(id);
        assertThat(message.getContext().getResult().orElseThrow()).isSameAs(value);
        assertThat(message.getContext().getState()).isSameAs(state);
        assertThat(message.getContext().getStartedAt()).isSameAs(startedAt);
        assertThat(message.getContext().getDuration()).isSameAs(duration);
    }

    @Test
    void shouldStoreCommandMessageLongBoolean() throws JsonProcessingException {
        long id = 2;
        boolean resultValue = true;
        Context.State state = Context.State.DONE;
        RootCommand<Boolean> command = mock(BooleanCommand.class);
        doReturn(DoCommandMessageTest.COMMAND_ID).when(command).getId();
        doReturn(BooleanCommand.class).when(command).commandFamily();
        CommandContext<Boolean> commandContext = CommandContext.<Boolean>builder().build();
        commandContext.setCommand(command);
        ActionContext actionContext = ActionContext.builder().actionName(TEST_ACTION).facadeName(TEST_FACADE).build();
        Instant startedAt = Instant.now();
        Duration duration = Duration.ofMillis(ChronoUnit.MILLIS.between(startedAt, Instant.now().plus(100, ChronoUnit.MILLIS)));
        // Create a DoCommandMessage instance
        // Set the command ID, processing context and command context
        DoCommandMessage<Boolean> message = DoCommandMessage.<Boolean>builder()
                .correlationId(CORRELATION_ID).actionContext(actionContext).context(commandContext)
                .build();

        // Set the parameter and result
        commandContext.setRedoParameter(new NumberIdParameter<>(id));
        commandContext.setState(Context.State.WORK);
        commandContext.setResult(resultValue);
        commandContext.setState(state);
        commandContext.setStartedAt(startedAt);
        commandContext.setDuration(duration);

        String json = objectMapper.writeValueAsString(message);

        assertThat(json).isNotBlank().contains(TEST_FACADE).contains(TEST_ACTION)
                .contains(NumberIdParameter.class.getName()).contains(BooleanResult.class.getName());

    }

    @Test
    void shouldRestoreCommandMessageLongBoolean() throws JsonProcessingException {
        String commandId = "student.findById";
        long id = 3;
        boolean resultValue = false;
        Context.State state = Context.State.DONE;
        CommandContext<Boolean> commandContext = CommandContext.<Boolean>builder().command(farm.command(commandId)).build();
        ActionContext actionContext = ActionContext.builder().actionName("test-processing").facadeName("test-facade").build();
        Instant startedAt = Instant.now();
        Duration duration = Duration.ofMillis(ChronoUnit.MILLIS.between(startedAt, Instant.now().plus(100, ChronoUnit.MILLIS)));

        // Create a DoCommandMessage instance
        // Set the command ID, processing context and command context
        DoCommandMessage<Boolean> message = DoCommandMessage.<Boolean>builder()
                .correlationId(CORRELATION_ID).actionContext(actionContext).context(commandContext)
                .build();

        // Set the parameter and result
        commandContext.setRedoParameter(new NumberIdParameter<>(id));
        commandContext.setState(Context.State.WORK);
        commandContext.setResult(resultValue);
        commandContext.setState(state);
        commandContext.setStartedAt(startedAt);
        commandContext.setDuration(duration);

        // Serialize the message to JSON
        String json = objectMapper.writeValueAsString(message);

        DoCommandMessage<Boolean> restoredMessage = objectMapper.readValue(json, DoCommandMessage.class);

        assertThat(restoredMessage).isNotNull().isEqualTo(message);
    }

    // inner types
    interface BooleanCommand extends RootCommand<Boolean> {
        // This interface is just a marker for commands that return Boolean
    }

    // private methods
    private <T> DoCommandMessage<T> createMessage(String correlationId, String commandId, Input<?> input) {
        CommandContext<T> context = CommandContext.<T>builder()
                .command(farm.command(commandId)).redoParameter(input)
                .startedAt(Instant.now()).duration(Duration.of(10, ChronoUnit.SECONDS))
                .build();
        context.setState(Context.State.INIT);
        context.setState(Context.State.READY);
        return DoCommandMessage.<T>builder()
                .correlationId(correlationId)
                .context(context)
                .actionContext(ActionContext.builder().actionName(TEST_ACTION).facadeName(TEST_FACADE).build())
                .build();
    }
}
