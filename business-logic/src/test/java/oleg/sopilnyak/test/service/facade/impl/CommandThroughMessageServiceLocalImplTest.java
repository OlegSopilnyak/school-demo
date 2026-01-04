package oleg.sopilnyak.test.service.facade.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.facade.impl.command.message.service.local.CommandThroughMessageServiceLocalImpl;
import oleg.sopilnyak.test.service.command.executable.core.executor.messaging.MessageProgressWatchdog;
import oleg.sopilnyak.test.service.facade.impl.command.message.MessagesProcessorAdapter;
import oleg.sopilnyak.test.service.message.CommandMessage;
import oleg.sopilnyak.test.service.message.DoCommandMessage;
import oleg.sopilnyak.test.service.message.UndoCommandMessage;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.awaitility.Durations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CommandThroughMessageServiceLocalImpl.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@SuppressWarnings("unchecked")
class CommandThroughMessageServiceLocalImplTest {
    @MockitoBean("commandsTroughMessageObjectMapper")
    private ObjectMapper objectMapper;
    @MockitoSpyBean
    @Autowired
    private CommandThroughMessageServiceLocalImpl service;

    ActionContext actionContext = ActionContext.builder().actionName("test-doingMainLoop").facadeName("test-facade").build();
    @Mock
    Context<?> commandContext;
    @Mock
    RootCommand<?> command;
    final String mockedCommandId = "test-command-id";
    final String messageJson = "spyied-base-message-json";

    @BeforeEach
    void setUp() throws JsonProcessingException {
        doReturn(mockedCommandId).when(command).getId();
        doReturn(messageJson).when(objectMapper).writeValueAsString(any(CommandMessage.class));
    }

    @Test
    void shouldCheckIntegrity() {
        assertThat(service).isNotNull();
        assertThat(((AtomicBoolean) ReflectionTestUtils.getField(service, "serviceActive")).get()).isTrue();
        ExecutorService controlExecutorService = (ExecutorService) ReflectionTestUtils.getField(service, "controlExecutorService");
        assertThat(controlExecutorService).isNotNull();
        assertThat(controlExecutorService.isShutdown()).isFalse();
        assertThat(controlExecutorService.isTerminated()).isFalse();
        ExecutorService operationalExecutorService = (ExecutorService) ReflectionTestUtils.getField(service, "messagesExecutorService");
        assertThat(operationalExecutorService).isNotNull();
        assertThat(operationalExecutorService.isShutdown()).isFalse();
        assertThat(operationalExecutorService.isTerminated()).isFalse();
        assertThat((Map) ReflectionTestUtils.getField(service, "messageInProgress")).isEmpty();
    }

    @Test
    void shouldStopService() {
        assertThat(((AtomicBoolean) ReflectionTestUtils.getField(service, "serviceActive")).get()).isTrue();
        ExecutorService controlExecutorService = (ExecutorService) ReflectionTestUtils.getField(service, "controlExecutorService");
        ExecutorService operationalExecutorService = (ExecutorService) ReflectionTestUtils.getField(service, "messagesExecutorService");
        assertThat(controlExecutorService).isNotNull();
        assertThat(controlExecutorService.isShutdown()).isFalse();
        assertThat(controlExecutorService.isTerminated()).isFalse();
        assertThat(operationalExecutorService).isNotNull();
        assertThat(operationalExecutorService.isShutdown()).isFalse();
        assertThat(operationalExecutorService.isTerminated()).isFalse();

        service.shutdown();

        assertThat(((AtomicBoolean) ReflectionTestUtils.getField(service, "serviceActive")).get()).isFalse();
        assertThat(controlExecutorService.isShutdown()).isTrue();
        assertThat(operationalExecutorService.isShutdown()).isTrue();
    }

    @Test
    <T> void shouldSendDoCommandMessage() throws JsonProcessingException {
        DoCommandMessage<T> message = spy(DoCommandMessage.<T>builder().correlationId("12345")
                .actionContext(actionContext).context((Context<T>) commandContext)
                .build());
        Map<String, MessageProgressWatchdog<?>> messageInProgress =
                (Map<String, MessageProgressWatchdog<?>>) ReflectionTestUtils.getField(service, "messageInProgress");
        assertThat(messageInProgress).isNotNull().isEmpty();

        MessagesProcessorAdapter requests = (MessagesProcessorAdapter) ReflectionTestUtils.getField(service, "inputProcessor");
        assertThat(requests).isNotNull();
        MessagesProcessorAdapter responses = (MessagesProcessorAdapter) ReflectionTestUtils.getField(service, "outputProcessor");
        assertThat(responses).isNotNull();
        doReturn(command).when(commandContext).getCommand();
        doReturn(message).when(objectMapper).readValue(eq(messageJson), any(Class.class));

        service.send(message);

        await().atMost(Durations.ONE_SECOND).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS).until(requests::isEmpty);
        await().atMost(Durations.ONE_SECOND).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS).until(responses::isEmpty);

        // check request message doingMainLoop
        verify(message, atLeastOnce()).getContext();

        // check command execution
        RootCommand<T> cmd = (RootCommand<T>) command;
        ArgumentCaptor<Context<T>> commandCaptor = ArgumentCaptor.forClass(Context.class);
        verify(cmd).doCommand(commandCaptor.capture());
        assertThat(commandContext).isEqualTo(commandCaptor.getValue());

        // check response message doingMainLoop
        MessageProgressWatchdog<?> messageWatchDog = messageInProgress.get(message.getCorrelationId());
        assertThat(messageWatchDog).isNotNull();
        assertThat(messageWatchDog.getState()).isEqualTo(MessageProgressWatchdog.State.COMPLETED);
        assertThat(messageWatchDog.getResult()).isEqualTo(message);
    }

    @Test
    void shouldNotSendCommandMessage_MessageAlreadyInProgress() {
        UndoCommandMessage message = spy(UndoCommandMessage.builder().correlationId("54321")
                .actionContext(actionContext).context(commandContext)
                .build());
        Map<String, MessageProgressWatchdog<?>> messageInProgress =
                (Map<String, MessageProgressWatchdog<?>>) ReflectionTestUtils.getField(service, "messageInProgress");
        assertThat(messageInProgress).isNotNull().isEmpty();
        // simulate that message is already in progress
        messageInProgress.put(message.getCorrelationId(), new MessageProgressWatchdog<>(message));

        service.send(message);

        // check request message doingMainLoop
        verify(message, never()).getContext();

        // check command execution
        verify(((RootCommand<Void>) command), never()).undoCommand(any(Context.class));

        // check response message doingMainLoop
        var messageWatchDog = messageInProgress.get(message.getCorrelationId());
        assertThat(messageWatchDog).isNotNull();
        assertThat(messageWatchDog.getState()).isEqualTo(MessageProgressWatchdog.State.IN_PROGRESS);
        assertThat(messageWatchDog.getResult()).isNull();
    }

    @Test
    <T> void shouldNotSendDoCommandMessage_ServiceStopped() {
        DoCommandMessage<T> message = spy(DoCommandMessage.<T>builder().correlationId("12345")
                .actionContext(actionContext).context((Context<T>) commandContext)
                .build());
        doReturn(command).when(commandContext).getCommand();
        service.shutdown();

        Exception e = assertThrows(UnableExecuteCommandException.class, () -> service.send(message));

        assertThat(e).isInstanceOf(UnableExecuteCommandException.class);
        assertThat(e.getMessage()).isEqualTo("Cannot execute command '" + mockedCommandId + "'");
        assertThat(e.getCause()).isInstanceOf(IllegalStateException.class);
        assertThat(e.getCause().getMessage()).isEqualTo("RequestMessagesProcessor isn't in active state.");
        // check request message doingMainLoop
        verify(message, atLeastOnce()).getContext();
    }

    @Test
    <T> void shouldSendUndoCommandMessage() throws JsonProcessingException {
        UndoCommandMessage message = spy(UndoCommandMessage.builder().correlationId("54321")
                .actionContext(actionContext).context(commandContext)
                .build());
        Map<String, MessageProgressWatchdog<?>> messageInProgress =
                (Map<String, MessageProgressWatchdog<?>>) ReflectionTestUtils.getField(service, "messageInProgress");
        assertThat(messageInProgress).isNotNull().isEmpty();

        MessagesProcessorAdapter requests = (MessagesProcessorAdapter) ReflectionTestUtils.getField(service, "inputProcessor");
        assertThat(requests).isNotNull();
        MessagesProcessorAdapter responses = (MessagesProcessorAdapter) ReflectionTestUtils.getField(service, "outputProcessor");
        assertThat(responses).isNotNull();
        doReturn(command).when(commandContext).getCommand();
        doReturn(message).when(objectMapper).readValue(eq(messageJson), any(Class.class));

        service.send(message);

        await().atMost(Durations.ONE_SECOND).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS).until(requests::isEmpty);
        await().atMost(Durations.ONE_SECOND).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS).until(responses::isEmpty);

        // check request message doingMainLoop
        verify(message, atLeastOnce()).getContext();

        // check command execution
        RootCommand<T> cmd = (RootCommand<T>) command;
        ArgumentCaptor<Context<T>> commandCaptor = ArgumentCaptor.forClass(Context.class);
        verify(cmd).undoCommand(commandCaptor.capture());
        assertThat(commandContext).isEqualTo(commandCaptor.getValue());

        // check response message doingMainLoop
        MessageProgressWatchdog<?> messageWatchDog = messageInProgress.get(message.getCorrelationId());
        assertThat(messageWatchDog).isNotNull();
        assertThat(messageWatchDog.getState()).isEqualTo(MessageProgressWatchdog.State.COMPLETED);
        assertThat(messageWatchDog.getResult()).isEqualTo(message);
    }

    @Test
    void shouldNotSendUndoCommandMessage_ServiceStopped() {
        UndoCommandMessage message = spy(UndoCommandMessage.builder().correlationId("12345")
                .actionContext(actionContext).context(commandContext)
                .build());
        doReturn(command).when(commandContext).getCommand();
        service.shutdown();

        Exception e = assertThrows(UnableExecuteCommandException.class, () -> service.send(message));

        assertThat(e).isInstanceOf(UnableExecuteCommandException.class);
        assertThat(e.getMessage()).isEqualTo("Cannot execute command '" + mockedCommandId + "'");
        assertThat(e.getCause()).isInstanceOf(IllegalStateException.class);
        assertThat(e.getCause().getMessage()).isEqualTo("RequestMessagesProcessor isn't in active state.");
        // check request message doingMainLoop
        verify(message, atLeastOnce()).getContext();
    }

    @Test
    <T> void shouldReceiveDoCommandMessage() throws JsonProcessingException {
        DoCommandMessage<T> message = spy(DoCommandMessage.<T>builder().correlationId("12345-12345")
                .actionContext(actionContext).context((Context<T>) commandContext)
                .build());
        Map<String, MessageProgressWatchdog<?>> messageInProgress =
                (Map<String, MessageProgressWatchdog<?>>) ReflectionTestUtils.getField(service, "messageInProgress");
        assertThat(messageInProgress).isNotNull().isEmpty();
        MessagesProcessorAdapter responses = (MessagesProcessorAdapter) ReflectionTestUtils.getField(service, "outputProcessor");
        assertThat(responses).isNotNull();
        doReturn(command).when(commandContext).getCommand();
        doReturn(message).when(objectMapper).readValue(eq(messageJson), any(Class.class));
        service.send(message);
        await().atMost(Durations.ONE_SECOND).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS).until(responses::isEmpty);
        assertThat(messageInProgress).containsKey(message.getCorrelationId());
        assertThat(messageInProgress.get(message.getCorrelationId()).getResult()).isEqualTo(message);
        // check command execution
        RootCommand<T> cmd = (RootCommand<T>) command;
        ArgumentCaptor<Context<T>> commandCaptor = ArgumentCaptor.forClass(Context.class);
        verify(cmd).doCommand(commandCaptor.capture());
        assertThat(commandContext).isEqualTo(commandCaptor.getValue());


        CommandMessage<T> result = service.receive(command.getId(), message.getCorrelationId());

        assertThat(result).isNotNull().isEqualTo(message);
        // check message doingMainLoop is finished
        assertThat(messageInProgress).isEmpty();
    }

    @Test
    <T> void shouldReceiveUndoCommandMessage() throws JsonProcessingException {
        UndoCommandMessage message = spy(UndoCommandMessage.builder().correlationId("54321-12345")
                .actionContext(actionContext).context(commandContext)
                .build());
        Map<String, MessageProgressWatchdog<?>> messageInProgress =
                (Map<String, MessageProgressWatchdog<?>>) ReflectionTestUtils.getField(service, "messageInProgress");
        assertThat(messageInProgress).isNotNull().isEmpty();
        MessagesProcessorAdapter responses = (MessagesProcessorAdapter) ReflectionTestUtils.getField(service, "outputProcessor");
        assertThat(responses).isNotNull();
        doReturn(command).when(commandContext).getCommand();
        doReturn(message).when(objectMapper).readValue(eq(messageJson), any(Class.class));
        service.send(message);
        await().atMost(Durations.ONE_SECOND).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS).until(responses::isEmpty);
        assertThat(messageInProgress).containsKey(message.getCorrelationId());
        assertThat(messageInProgress.get(message.getCorrelationId()).getResult()).isEqualTo(message);
        // check command execution
        RootCommand<T> cmd = (RootCommand<T>) command;
        ArgumentCaptor<Context<T>> commandCaptor = ArgumentCaptor.forClass(Context.class);
        verify(cmd).undoCommand(commandCaptor.capture());
        assertThat(commandContext).isEqualTo(commandCaptor.getValue());


        CommandMessage<T> result = service.receive(command.getId(), message.getCorrelationId());

        assertThat(result).isNotNull().isEqualTo(message);
        // check message doingMainLoop is finished
        assertThat(messageInProgress).isEmpty();
    }

    @Test
    void shouldNotReceiveCommandMessage_MessageIsNotInProgress() {
        UndoCommandMessage message = spy(UndoCommandMessage.builder().correlationId("54321-54321-12345")
                .actionContext(actionContext).context(commandContext)
                .build());

        CommandMessage<?> result = service.receive(command.getId(), message.getCorrelationId());

        assertThat(result).isNull();
    }

    @Test
    void shouldNotReceiveCommandMessage_ServiceStopped() {
        UndoCommandMessage message = spy(UndoCommandMessage.builder().correlationId("54321-54321-12345")
                .actionContext(actionContext).context(commandContext)
                .build());
        String commandId = command.getId();
        String correlationId = message.getCorrelationId();
        service.shutdown();

        Exception e = assertThrows(UnableExecuteCommandException.class, () -> service.receive(commandId, correlationId));

        assertThat(e).isInstanceOf(UnableExecuteCommandException.class);
        assertThat(e.getMessage()).isEqualTo("Cannot execute command '" + mockedCommandId + "'");
        assertThat(e.getCause()).isInstanceOf(IllegalStateException.class);
        assertThat(e.getCause().getMessage()).isEqualTo("ResponseMessagesProcessor is NOT active.");
    }
}