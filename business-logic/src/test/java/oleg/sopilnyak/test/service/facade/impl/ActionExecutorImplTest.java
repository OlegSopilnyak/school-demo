package oleg.sopilnyak.test.service.facade.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.facade.impl.command.message.service.local.CommandThroughMessageServiceLocalImpl;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandMessage;
import oleg.sopilnyak.test.service.message.CommandThroughMessageService;
import oleg.sopilnyak.test.service.message.DoCommandMessage;
import oleg.sopilnyak.test.service.message.UndoCommandMessage;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ActionExecutorImplTest<T> {
    @Spy
    @InjectMocks
    ActionExecutorImpl actionExecutor;
    @Mock
    ApplicationContext applicationContext;
    @Mock
    ObjectMapper objectMapper;
    @Spy
    @InjectMocks
    CommandThroughMessageServiceLocalImpl messagesExchangeService;

    ActionContext actionContext = ActionContext.builder().actionName("test-doingMainLoop").facadeName("test-facade").build();
    @Mock
    Context<T> commandContext;
    @Mock
    RootCommand<T> command;
    static final String MOCKED_COMMAND_ID = "mocked-command-id";
    static final String MESSAGE_JSON = "base-message-json";

    @BeforeEach
    void setUp() throws JsonProcessingException {
        messagesExchangeService.initialize();
        ReflectionTestUtils.setField(actionExecutor, "messagesExchangeService", messagesExchangeService);
        doReturn(messagesExchangeService).when(applicationContext).getBean(CommandThroughMessageService.class);
        doReturn(MOCKED_COMMAND_ID).when(command).getId();
        doReturn(MESSAGE_JSON).when(objectMapper).writeValueAsString(any(CommandMessage.class));
    }

    @AfterEach
    void tearDown() {
        messagesExchangeService.shutdown();
    }

    @Test
    void shouldCommitAction() throws JsonProcessingException {
        reset(applicationContext);
        doReturn(command).when(commandContext).getCommand();
        CommandMessage response = mock(CommandMessage.class);
        doReturn(commandContext).when(response).getContext();
        doAnswer(invocationOnMock -> {
            final CommandMessage message = invocationOnMock.getArgument(0, CommandMessage.class);
            doReturn(message.getDirection()).when(response).getDirection();
            doReturn(message.getCorrelationId()).when(response).getCorrelationId();
            return invocationOnMock.callRealMethod();
        }).when(messagesExchangeService).send(any(CommandMessage.class));
        // prepare message-response
        doReturn(response).when(objectMapper).readValue(eq(MESSAGE_JSON), any(Class.class));

        Context<?> context = actionExecutor.commitAction(actionContext, commandContext);

        assertThat(context).isNotNull();
        verify(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        verify(messagesExchangeService).send(any(BaseCommandMessage.class));
        verify(command).doCommand(commandContext);
    }

    @Test
    void shouldRollbackAction() throws JsonProcessingException {
        reset(applicationContext);
        doReturn(command).when(commandContext).getCommand();
        CommandMessage response = mock(CommandMessage.class);
        doReturn(commandContext).when(response).getContext();
        doAnswer(invocationOnMock -> {
            final CommandMessage commandMessage = invocationOnMock.getArgument(0, CommandMessage.class);
            doReturn(commandMessage.getDirection()).when(response).getDirection();
            doReturn(commandMessage.getCorrelationId()).when(response).getCorrelationId();
            return invocationOnMock.callRealMethod();
        }).when(messagesExchangeService).send(any(CommandMessage.class));
        // prepare message-response
        doReturn(response).when(objectMapper).readValue(eq(MESSAGE_JSON), any(Class.class));

        Context<?> context = actionExecutor.rollbackAction(actionContext, commandContext);

        assertThat(context).isNotNull();
        verify(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        verify(command).undoCommand(commandContext);
    }

    @Test
    void shouldProcessDoActionCommand() throws JsonProcessingException {
        reset(applicationContext);
        DoCommandMessage<T> message = DoCommandMessage.<T>builder()
                .actionContext(actionContext).context(commandContext)
                .correlationId(UUID.randomUUID().toString())
                .build();
        doReturn(command).when(commandContext).getCommand();
        CommandMessage response = mock(CommandMessage.class);
        doReturn(message.getDirection()).when(response).getDirection();
        doReturn(message.getCorrelationId()).when(response).getCorrelationId();
        doReturn(commandContext).when(response).getContext();
        // prepare message-response
        doReturn(response).when(objectMapper).readValue(eq(MESSAGE_JSON), any(Class.class));

        CommandMessage<T> processed = actionExecutor.processActionCommand(message);

        assertThat(processed).isNotNull().isEqualTo(response);
        verify(command).doCommand(commandContext);
    }

    @Test
    void shouldProcessUndoActionCommand() throws JsonProcessingException {
        reset(applicationContext);
        UndoCommandMessage message = UndoCommandMessage.builder()
                .actionContext(actionContext).context(commandContext)
                .correlationId(UUID.randomUUID().toString())
                .build();
        doReturn(command).when(commandContext).getCommand();
        doReturn(message).when(objectMapper).readValue(eq(MESSAGE_JSON), any(Class.class));

        CommandMessage<Void> processed = actionExecutor.processActionCommand(message);

        assertThat(processed).isNotNull().isEqualTo(message);
        verify(command).undoCommand(commandContext);
    }

    @Test
    void shouldNotProcessActionCommand_UnknownDirection() {
        reset(applicationContext, objectMapper, command);
        BaseCommandMessage<?> message = new BaseCommandMessage<>("correlation-id", actionContext, commandContext) {
            @Override
            public Direction getDirection() {
                return Direction.UNKNOWN;
            }
        };

        Exception e = assertThrows(IllegalArgumentException.class, () -> actionExecutor.processActionCommand(message));

        assertThat(e).isInstanceOf(IllegalArgumentException.class);
        assertThat(e.getMessage()).isEqualTo("Unknown message direction: UNKNOWN");
        verify(command, never()).undoCommand(commandContext);
        verify(command, never()).doCommand(commandContext);
        verify(actionExecutor, never()).getLogger();
    }

    @Test
    void shouldNotProcessActionCommand_NullDirection() {
        reset(applicationContext, objectMapper, command);
        BaseCommandMessage<?> message = new BaseCommandMessage<>("correlation-id", actionContext, commandContext) {
            @Override
            public Direction getDirection() {
                return null;
            }
        };

        Exception e = assertThrows(IllegalArgumentException.class, () -> actionExecutor.processActionCommand(message));

        assertThat(e).isInstanceOf(IllegalArgumentException.class);
        assertThat(e.getMessage()).isEqualTo("Message direction is not defined.");
        verify(command, never()).undoCommand(commandContext);
        verify(command, never()).doCommand(commandContext);
        verify(actionExecutor, never()).getLogger();
    }
}
