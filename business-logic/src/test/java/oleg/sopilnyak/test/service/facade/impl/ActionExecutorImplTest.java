package oleg.sopilnyak.test.service.facade.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
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

@ExtendWith(MockitoExtension.class)
class ActionExecutorImplTest<T> {
    @Spy
    @InjectMocks
    ActionExecutorImpl actionExecutor;
    @Mock
    ApplicationContext applicationContext;
    @Spy
    @InjectMocks
    CommandThroughMessageServiceLocalImpl messagesExchangeService;

    ActionContext actionContext = ActionContext.builder().actionName("test-processing").facadeName("test-facade").build();
    @Mock
    Context<T> commandContext;
    @Mock
    RootCommand<T> command;

    @BeforeEach
    void setUp() {
        messagesExchangeService.initialize();
        ReflectionTestUtils.setField(actionExecutor, "messagesExchangeService", messagesExchangeService);
        doReturn(messagesExchangeService).when(applicationContext).getBean(CommandThroughMessageService.class);
    }

    @AfterEach
    void tearDown() {
        messagesExchangeService.shutdown();
    }

    @Test
    void shouldCommitAction() {
        reset(applicationContext);
        doReturn(command).when(commandContext).getCommand();

        Context<?> context = actionExecutor.commitAction(actionContext, commandContext);

        assertThat(context).isNotNull();
        verify(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        verify(messagesExchangeService).send(any(BaseCommandMessage.class));
        verify(command).doCommand(commandContext);
    }

    @Test
    void shouldRollbackAction() {
        reset(applicationContext);
        doReturn(command).when(commandContext).getCommand();

        Context<?> context = actionExecutor.rollbackAction(actionContext, commandContext);

        assertThat(context).isNotNull();
        verify(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        verify(command).undoCommand(commandContext);
    }

    @Test
    void shouldProcessDoActionCommand() {
        reset(applicationContext);
        DoCommandMessage<T> message = DoCommandMessage.<T>builder()
                .actionContext(actionContext).context(commandContext)
                .correlationId(UUID.randomUUID().toString())
                .build();
        doReturn(command).when(commandContext).getCommand();

        CommandMessage<T> processed = actionExecutor.processActionCommand(message);

        assertThat(processed).isNotNull().isEqualTo(message);
        verify(command).doCommand(commandContext);
    }

    @Test
    void shouldProcessUndoActionCommand() {
        reset(applicationContext);
        UndoCommandMessage message = UndoCommandMessage.builder()
                .actionContext(actionContext).context(commandContext)
                .correlationId(UUID.randomUUID().toString())
                .build();
        doReturn(command).when(commandContext).getCommand();

        CommandMessage<Void> processed = actionExecutor.processActionCommand(message);

        assertThat(processed).isNotNull().isEqualTo(message);
        verify(command).undoCommand(commandContext);
    }

    @Test
    void shouldNotProcessActionCommand_UnknownDirection() {
        reset(applicationContext);
        BaseCommandMessage<?> message = new BaseCommandMessage<>("correlation-id", actionContext, commandContext) {
            @Override
            public Direction getDirection() {
                return Direction.UNKNOWN;
            }
        };
        doReturn(command).when(commandContext).getCommand();

        Exception e = assertThrows(IllegalArgumentException.class, () -> actionExecutor.processActionCommand(message));

        assertThat(e).isInstanceOf(IllegalArgumentException.class);
        assertThat(e.getMessage()).isEqualTo("Unknown message direction: UNKNOWN");
        verify(command, never()).undoCommand(commandContext);
        verify(command, never()).doCommand(commandContext);
        verify(actionExecutor, never()).getLogger();
    }

    @Test
    void shouldNotProcessActionCommand_NullDirection() {
        reset(applicationContext);
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
