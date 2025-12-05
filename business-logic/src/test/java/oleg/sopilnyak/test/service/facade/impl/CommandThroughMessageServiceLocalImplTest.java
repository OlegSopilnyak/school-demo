package oleg.sopilnyak.test.service.facade.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.facade.impl.message.MessageProgressWatchdog;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.DoCommandMessage;
import oleg.sopilnyak.test.service.message.UndoCommandMessage;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = {CommandThroughMessageServiceLocalImpl.class})
@TestPropertySource(properties = {"school.maximum.threads.pool.size=9"})
class CommandThroughMessageServiceLocalImplTest {
    @MockitoBean
    PlatformTransactionManager platformTransactionManager;

    @MockitoSpyBean
    @Autowired
    private CommandThroughMessageServiceLocalImpl service;

    ActionContext actionContext = ActionContext.builder().actionName("test-processing").facadeName("test-facade").build();
    @Mock
    Context<?> commandContext;
    @Mock
    RootCommand<?> command;
    final String mockedCommandId = "test-command-id";

    @BeforeEach
    void setUp() {
        doReturn(mockedCommandId).when(command).getId();
    }

    @Test
    void shouldCheckIntegrity() {
        assertThat(service).isNotNull();
        assertThat(ReflectionTestUtils.getField(service, "maximumPoolSize"))
                .isEqualTo(9)
                .isNotEqualTo(Runtime.getRuntime().availableProcessors());
        assertThat(((AtomicBoolean) ReflectionTestUtils.getField(service, "serviceActive")).get()).isTrue();
        ExecutorService controlExecutorService = (ExecutorService) ReflectionTestUtils.getField(service, "controlExecutorService");
        assertThat(controlExecutorService).isNotNull();
        assertThat(controlExecutorService.isShutdown()).isFalse();
        assertThat(controlExecutorService.isTerminated()).isFalse();
        ExecutorService operationalExecutorService = (ExecutorService) ReflectionTestUtils.getField(service, "operationalExecutorService");
        assertThat(operationalExecutorService).isNotNull();
        assertThat(operationalExecutorService.isShutdown()).isFalse();
        assertThat(operationalExecutorService.isTerminated()).isFalse();
        assertThat((Map) ReflectionTestUtils.getField(service, "messageInProgress")).isEmpty();
    }

    @Test
    void shouldStopService() {
        assertThat(((AtomicBoolean) ReflectionTestUtils.getField(service, "serviceActive")).get()).isTrue();
        ExecutorService controlExecutorService = (ExecutorService) ReflectionTestUtils.getField(service, "controlExecutorService");
        ExecutorService operationalExecutorService = (ExecutorService) ReflectionTestUtils.getField(service, "operationalExecutorService");
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
    <T> void shouldSendDoCommandMessage() {
        DoCommandMessage<T> message = spy(DoCommandMessage.<T>builder().correlationId("12345")
                .actionContext(actionContext).context((Context<T>) commandContext)
                .build());
        Map<String, MessageProgressWatchdog<?>> messageInProgress =
                (Map<String, MessageProgressWatchdog<?>>) ReflectionTestUtils.getField(service, "messageInProgress");
        assertThat(messageInProgress).isNotNull().isEmpty();

        BlockingQueue<BaseCommandMessage<?>> requests =
                (BlockingQueue<BaseCommandMessage<?>>) ReflectionTestUtils.getField(service, "requests");
        assertThat(requests).isNotNull().isEmpty();
        BlockingQueue<BaseCommandMessage<?>> responses =
                (BlockingQueue<BaseCommandMessage<?>>) ReflectionTestUtils.getField(service, "responses");
        assertThat(responses).isNotNull().isEmpty();
        doReturn(command).when(commandContext).getCommand();

        service.send(message);

        await().atMost(Durations.ONE_SECOND).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS)
                .until(requests::isEmpty);
        await().atMost(Durations.ONE_SECOND).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS)
                .until(responses::isEmpty);

        // check request message processing
        verify(message, atLeastOnce()).getContext();

        // check command execution
        RootCommand<T> cmd = (RootCommand<T>) command;
        ArgumentCaptor<Context<T>> commandCaptor = ArgumentCaptor.forClass(Context.class);
        verify(cmd).doCommand(commandCaptor.capture());
        assertThat(commandContext).isEqualTo(commandCaptor.getValue());

        // check response message processing
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
        messageInProgress.put(message.getCorrelationId(), new MessageProgressWatchdog<>());

        service.send(message);

        // check request message processing
        verify(message, never()).getContext();

        // check command execution
        verify(((RootCommand<Void>) command), never()).undoCommand(any(Context.class));

        // check response message processing
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
        assertThat(e.getCause().getMessage()).isEqualTo("RequestMessagesProcessor is NOT active.");
        // check request message processing
        verify(message, atLeastOnce()).getContext();
    }

    @Test
    <T> void shouldSendUndoCommandMessage() {
        UndoCommandMessage message = spy(UndoCommandMessage.builder().correlationId("54321")
                .actionContext(actionContext).context(commandContext)
                .build());
        Map<String, MessageProgressWatchdog<?>> messageInProgress =
                (Map<String, MessageProgressWatchdog<?>>) ReflectionTestUtils.getField(service, "messageInProgress");
        assertThat(messageInProgress).isNotNull().isEmpty();

        BlockingQueue<BaseCommandMessage<?>> requests =
                (BlockingQueue<BaseCommandMessage<?>>) ReflectionTestUtils.getField(service, "requests");
        assertThat(requests).isNotNull().isEmpty();
        BlockingQueue<BaseCommandMessage<?>> responses =
                (BlockingQueue<BaseCommandMessage<?>>) ReflectionTestUtils.getField(service, "responses");
        assertThat(responses).isNotNull().isEmpty();
        doReturn(command).when(commandContext).getCommand();

        service.send(message);

        await().atMost(Durations.ONE_SECOND).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS)
                .until(requests::isEmpty);
        await().atMost(Durations.ONE_SECOND).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS)
                .until(responses::isEmpty);

        // check request message processing
        verify(message, atLeastOnce()).getContext();

        // check command execution
        RootCommand<T> cmd = (RootCommand<T>) command;
        ArgumentCaptor<Context<T>> commandCaptor = ArgumentCaptor.forClass(Context.class);
        verify(cmd).undoCommand(commandCaptor.capture());
        assertThat(commandContext).isEqualTo(commandCaptor.getValue());

        // check response message processing
        MessageProgressWatchdog<?> messageWatchDog =
                messageInProgress.get(message.getCorrelationId());
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
        assertThat(e.getCause().getMessage()).isEqualTo("RequestMessagesProcessor is NOT active.");
        // check request message processing
        verify(message, atLeastOnce()).getContext();
    }

    @Test
    <T> void shouldReceiveDoCommandMessage() {
        DoCommandMessage<T> message = spy(DoCommandMessage.<T>builder().correlationId("12345-12345")
                .actionContext(actionContext).context((Context<T>) commandContext)
                .build());
        Map<String, MessageProgressWatchdog<?>> messageInProgress =
                (Map<String, MessageProgressWatchdog<?>>) ReflectionTestUtils.getField(service, "messageInProgress");
        assertThat(messageInProgress).isNotNull().isEmpty();
        BlockingQueue<BaseCommandMessage<?>> responses =
                (BlockingQueue<BaseCommandMessage<?>>) ReflectionTestUtils.getField(service, "responses");
        assertThat(responses).isNotNull().isEmpty();
        doReturn(command).when(commandContext).getCommand();
        service.send(message);
        await().atMost(Durations.ONE_SECOND).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS)
                .until(responses::isEmpty);
        assertThat(messageInProgress).containsKey(message.getCorrelationId());
        assertThat(messageInProgress.get(message.getCorrelationId()).getResult()).isEqualTo(message);
        // check command execution
        RootCommand<T> cmd = (RootCommand<T>) command;
        ArgumentCaptor<Context<T>> commandCaptor = ArgumentCaptor.forClass(Context.class);
        verify(cmd).doCommand(commandCaptor.capture());
        assertThat(commandContext).isEqualTo(commandCaptor.getValue());


        BaseCommandMessage<T> result = service.receive(command.getId(), message.getCorrelationId());

        assertThat(result).isNotNull().isEqualTo(message);
        // check message processing is finished
        assertThat(messageInProgress).isEmpty();
    }

    @Test
    <T> void shouldReceiveUndoCommandMessage() {
        UndoCommandMessage message = spy(UndoCommandMessage.builder().correlationId("54321-12345")
                .actionContext(actionContext).context(commandContext)
                .build());
        Map<String, MessageProgressWatchdog<?>> messageInProgress =
                (Map<String, MessageProgressWatchdog<?>>) ReflectionTestUtils.getField(service, "messageInProgress");
        assertThat(messageInProgress).isNotNull().isEmpty();
        BlockingQueue<BaseCommandMessage<?>> responses =
                (BlockingQueue<BaseCommandMessage<?>>) ReflectionTestUtils.getField(service, "responses");
        assertThat(responses).isNotNull().isEmpty();
        doReturn(command).when(commandContext).getCommand();
        service.send(message);
        await().atMost(Durations.ONE_SECOND).pollInterval(Durations.ONE_HUNDRED_MILLISECONDS)
                .until(responses::isEmpty);
        assertThat(messageInProgress).containsKey(message.getCorrelationId());
        assertThat(messageInProgress.get(message.getCorrelationId()).getResult()).isEqualTo(message);
        // check command execution
        RootCommand<T> cmd = (RootCommand<T>) command;
        ArgumentCaptor<Context<T>> commandCaptor = ArgumentCaptor.forClass(Context.class);
        verify(cmd).undoCommand(commandCaptor.capture());
        assertThat(commandContext).isEqualTo(commandCaptor.getValue());


        BaseCommandMessage<T> result = service.receive(command.getId(), message.getCorrelationId());

        assertThat(result).isNotNull().isEqualTo(message);
        // check message processing is finished
        assertThat(messageInProgress).isEmpty();
    }

    @Test
    void shouldNotReceiveCommandMessage_MessageIsNotInProgress() {
        UndoCommandMessage message = spy(UndoCommandMessage.builder().correlationId("54321-54321-12345")
                .actionContext(actionContext).context(commandContext)
                .build());

        BaseCommandMessage<?> result = service.receive(command.getId(), message.getCorrelationId());

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