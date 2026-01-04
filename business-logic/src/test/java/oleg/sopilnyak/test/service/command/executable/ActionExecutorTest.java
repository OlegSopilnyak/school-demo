package oleg.sopilnyak.test.service.command.executable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.CommandMessage;
import oleg.sopilnyak.test.service.message.DoCommandMessage;
import oleg.sopilnyak.test.service.message.UndoCommandMessage;

import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ActionExecutorTest<T> {
    CommandActionExecutor actionExecutor;

    @Mock
    Logger logger;

    ActionContext actionContext = ActionContext.builder().actionName("base-test-doingMainLoop").facadeName("base-test-facade").build();
    @Mock
    Context<T> commandContext;
    @Mock
    RootCommand<T> command;

    @BeforeEach
    void setUp() {
        actionExecutor = spy(new CommandActionExecutor() {
            @Override
            public Logger getLogger() {
                return logger;
            }
        });
        // getting command from context
        doReturn(command).when(commandContext).getCommand();
    }

    @Test
    void shouldValidateInputMessage() {
        reset(commandContext);
        DoCommandMessage<T> execute = DoCommandMessage.<T>builder()
                .actionContext(actionContext).context(commandContext)
                .correlationId(UUID.randomUUID().toString())
                .build();
        // Act & Assert
        Assertions.assertDoesNotThrow(() -> {
            // The code that is expected to run without throwing any exceptions
            actionExecutor.validateInput(execute);
        });
        //
        UndoCommandMessage rollback = UndoCommandMessage.builder()
                .actionContext(actionContext).context(commandContext)
                .correlationId(UUID.randomUUID().toString())
                .build();

        // Act & Assert
        Assertions.assertDoesNotThrow(() -> {
            // The code that is expected to run without throwing any exceptions
            actionExecutor.validateInput(rollback);
        });
    }

    @Test
    void shouldCommitAction() {

        Context<?> context = actionExecutor.commitAction(actionContext, commandContext);

        assertThat(context).isNotNull();
        verify(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        verify(command).doCommand(commandContext);
    }

    @Test
    void shouldRollbackAction() {
        Context<?> context = actionExecutor.rollbackAction(actionContext, commandContext);

        assertThat(context).isNotNull();
        verify(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        verify(command).undoCommand(commandContext);
    }

    @Test
    void shouldProcessDoActionCommand() {
        DoCommandMessage<T> message = DoCommandMessage.<T>builder()
                .actionContext(actionContext).context(commandContext)
                .correlationId(UUID.randomUUID().toString())
                .build();

        CommandMessage<T> processed = actionExecutor.processActionCommand(message);

        assertThat(processed).isNotNull().isEqualTo(message);
        verify(command).doCommand(commandContext);
    }

    @Test
    void shouldProcessUndoActionCommand() {
        UndoCommandMessage message = UndoCommandMessage.builder()
                .actionContext(actionContext).context(commandContext)
                .correlationId(UUID.randomUUID().toString())
                .build();

        CommandMessage<Void> processed = actionExecutor.processActionCommand(message);

        assertThat(processed).isNotNull().isEqualTo(message);
        verify(command).undoCommand(commandContext);
    }

    @Test
    void shouldNotProcessActionCommand_UnknownDirection() {
        BaseCommandMessage<?> message = new BaseCommandMessage<>("correlation-id", actionContext, commandContext) {
            @Override
            public Direction getDirection() {
                return Direction.UNKNOWN;
            }
        };

        actionExecutor.processActionCommand(message);

        verify(command, never()).undoCommand(commandContext);
        verify(command, never()).doCommand(commandContext);
        verify(actionExecutor, atLeastOnce()).getLogger();
        verify(logger).warn("Unknown message direction: '{}' for command '{}'.", message.getDirection(), command.getId());

        ArgumentCaptor<IllegalArgumentException> captor = ArgumentCaptor.forClass(IllegalArgumentException.class);
        verify(commandContext).failed(captor.capture());
        assertThat(captor.getValue().getMessage()).isEqualTo("Unknown message direction: UNKNOWN");
    }

    @Test
    void shouldNotProcessActionCommand_NullDirection() {
        BaseCommandMessage<?> message = new BaseCommandMessage<>("correlation-id", actionContext, commandContext) {
            @Override
            public Direction getDirection() {
                return null;
            }
        };

        actionExecutor.processActionCommand(message);

        verify(command, never()).undoCommand(commandContext);
        verify(command, never()).doCommand(commandContext);
        verify(actionExecutor, atLeastOnce()).getLogger();
        verify(logger).warn("Command message direction is not defined in: '{}'.", message);

        ArgumentCaptor<IllegalArgumentException> captor = ArgumentCaptor.forClass(IllegalArgumentException.class);
        verify(commandContext).failed(captor.capture());
        assertThat(captor.getValue().getMessage()).isEqualTo("Command message direction is not defined properly.");
    }
}
