package oleg.sopilnyak.test.service.facade.impl;

import static oleg.sopilnyak.test.service.facade.impl.CommandThroughMessageServiceLocalImpl.State.COMPLETED;
import static oleg.sopilnyak.test.service.facade.impl.CommandThroughMessageServiceLocalImpl.State.IN_PROGRESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.DoCommandMessage;
import oleg.sopilnyak.test.service.message.UndoCommandMessage;
import org.awaitility.Durations;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = {CommandThroughMessageServiceLocalImpl.class})
@TestPropertySource(properties = {"school.maximum.threads.pool.size=9"})
class CommandThroughMessageServiceLocalImplTest {

    @SpyBean
    @Autowired
    private CommandThroughMessageServiceLocalImpl service;

    ActionContext actionContext = ActionContext.builder().actionName("test-action").facadeName("test-facade").build();
    @Mock
    Context<?> commandContext;
    @Mock
    RootCommand<?> command;

    @Test
    void shouldCheckIntegrity() {
        assertThat(service).isNotNull();
        assertThat(ReflectionTestUtils.getField(service, "maximumPoolSize"))
                .isEqualTo(9)
                .isNotEqualTo(Runtime.getRuntime().availableProcessors());
        assertThat(((AtomicBoolean)ReflectionTestUtils.getField(service, "serviceActive")).get()).isTrue();
        ThreadPoolTaskExecutor controlExecutorService = (ThreadPoolTaskExecutor) ReflectionTestUtils.getField(service, "controlExecutorService");
        assertThat(controlExecutorService).isNotNull();
        assertThat(controlExecutorService.getThreadPoolExecutor().isShutdown()).isFalse();
        assertThat(controlExecutorService.getThreadPoolExecutor().isTerminated()).isFalse();
        ThreadPoolTaskExecutor operationalExecutorService = (ThreadPoolTaskExecutor) ReflectionTestUtils.getField(service, "operationalExecutorService");
        assertThat(operationalExecutorService).isNotNull();
        assertThat(operationalExecutorService.getThreadPoolExecutor().isShutdown()).isFalse();
        assertThat(operationalExecutorService.getThreadPoolExecutor().isTerminated()).isFalse();
        assertThat((Map) ReflectionTestUtils.getField(service, "messageInProgress")).isEmpty();
    }

    @Test
    void shouldStopService() {
        assertThat(((AtomicBoolean)ReflectionTestUtils.getField(service, "serviceActive")).get()).isTrue();
        ThreadPoolTaskExecutor controlExecutorService = (ThreadPoolTaskExecutor) ReflectionTestUtils.getField(service, "controlExecutorService");
        ThreadPoolTaskExecutor operationalExecutorService = (ThreadPoolTaskExecutor) ReflectionTestUtils.getField(service, "operationalExecutorService");
        assertThat(controlExecutorService).isNotNull();
        assertThat(controlExecutorService.getThreadPoolExecutor().isShutdown()).isFalse();
        assertThat(controlExecutorService.getThreadPoolExecutor().isTerminated()).isFalse();
        assertThat(operationalExecutorService).isNotNull();
        assertThat(operationalExecutorService.getThreadPoolExecutor().isShutdown()).isFalse();
        assertThat(operationalExecutorService.getThreadPoolExecutor().isTerminated()).isFalse();

        service.stopService();

        assertThat(((AtomicBoolean)ReflectionTestUtils.getField(service, "serviceActive")).get()).isFalse();
        assertThat(controlExecutorService.getThreadPoolExecutor().isShutdown()).isTrue();
        assertThat(operationalExecutorService.getThreadPoolExecutor().isShutdown()).isTrue();
    }

    @Test
    <T> void shouldSendDoCommandMessage() {
        DoCommandMessage message = spy(DoCommandMessage.builder().correlationId("12345")
                .actionContext(actionContext).context((Context<Object>) commandContext)
                .build());
        Map<String, CommandThroughMessageServiceLocalImpl.MessageInProgress<?>> messageInProgress =
                (Map<String, CommandThroughMessageServiceLocalImpl.MessageInProgress<?>>) ReflectionTestUtils.getField(service, "messageInProgress");
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
        CommandThroughMessageServiceLocalImpl.MessageInProgress<?> messageWatchDog =
                messageInProgress.get(message.getCorrelationId());
        assertThat(messageWatchDog).isNotNull();
        assertThat(messageWatchDog.getState()).isEqualTo(COMPLETED);
        assertThat(messageWatchDog.getResult()).isEqualTo(message);
    }

    @Test
    <T> void shouldSendUndoCommandMessage() {
        UndoCommandMessage message = spy(UndoCommandMessage.builder().correlationId("54321")
                .actionContext(actionContext).context((Context<Void>) commandContext)
                .build());
        Map<String, CommandThroughMessageServiceLocalImpl.MessageInProgress<?>> messageInProgress =
                (Map<String, CommandThroughMessageServiceLocalImpl.MessageInProgress<?>>) ReflectionTestUtils.getField(service, "messageInProgress");
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
        CommandThroughMessageServiceLocalImpl.MessageInProgress<?> messageWatchDog =
                messageInProgress.get(message.getCorrelationId());
        assertThat(messageWatchDog).isNotNull();
        assertThat(messageWatchDog.getState()).isEqualTo(COMPLETED);
        assertThat(messageWatchDog.getResult()).isEqualTo(message);
    }

    @Test
    void shouldNotSendCommandMessage_MessageAlreadyInProgress() {
        UndoCommandMessage message = spy(UndoCommandMessage.builder().correlationId("54321")
                .actionContext(actionContext).context((Context<Void>) commandContext)
                .build());
        Map<String, CommandThroughMessageServiceLocalImpl.MessageInProgress<?>> messageInProgress =
                (Map<String, CommandThroughMessageServiceLocalImpl.MessageInProgress<?>>) ReflectionTestUtils.getField(service, "messageInProgress");
        assertThat(messageInProgress).isNotNull().isEmpty();
        // simulate that message is already in progress
        messageInProgress.put(message.getCorrelationId(), new CommandThroughMessageServiceLocalImpl.MessageInProgress<>());

        service.send(message);

        // check request message processing
        verify(message, never()).getContext();

        // check command execution
        verify(((RootCommand<Void>)command), never()).undoCommand(any(Context.class));

        // check response message processing
        var messageWatchDog = messageInProgress.get(message.getCorrelationId());
        assertThat(messageWatchDog).isNotNull();
        assertThat(messageWatchDog.getState()).isEqualTo(IN_PROGRESS);
        assertThat(messageWatchDog.getResult()).isNull();
    }

    @Test
    <T> void shouldReceiveDoCommandMessage() {
        DoCommandMessage message = spy(DoCommandMessage.builder().correlationId("12345-12345")
                .actionContext(actionContext).context((Context<Object>) commandContext)
                .build());
        Map<String, CommandThroughMessageServiceLocalImpl.MessageInProgress<?>> messageInProgress =
                (Map<String, CommandThroughMessageServiceLocalImpl.MessageInProgress<?>>) ReflectionTestUtils.getField(service, "messageInProgress");
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


        BaseCommandMessage result = service.receive(message.getCorrelationId());

        assertThat(result).isNotNull().isEqualTo(message);
        // check message processing is finished
        assertThat(messageInProgress).isEmpty();
    }

    @Test
    <T> void shouldReceiveUndoCommandMessage() {
        UndoCommandMessage message = spy(UndoCommandMessage.builder().correlationId("54321-12345")
                .actionContext(actionContext).context((Context<Void>) commandContext)
                .build());
        Map<String, CommandThroughMessageServiceLocalImpl.MessageInProgress<?>> messageInProgress =
                (Map<String, CommandThroughMessageServiceLocalImpl.MessageInProgress<?>>) ReflectionTestUtils.getField(service, "messageInProgress");
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


        BaseCommandMessage result = service.receive(message.getCorrelationId());

        assertThat(result).isNotNull().isEqualTo(message);
        // check message processing is finished
        assertThat(messageInProgress).isEmpty();
    }

    @Test
    void shouldNotReceiveCommandMessage_MessageIsNotInProgress() {
        UndoCommandMessage message = spy(UndoCommandMessage.builder().correlationId("54321-54321-12345")
                .actionContext(actionContext).context((Context<Void>) commandContext)
                .build());

        BaseCommandMessage result = service.receive(message.getCorrelationId());

        assertThat(result).isNull();
    }
}