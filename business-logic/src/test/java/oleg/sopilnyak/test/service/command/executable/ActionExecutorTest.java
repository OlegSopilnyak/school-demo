package oleg.sopilnyak.test.service.command.executable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.UUID;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
import oleg.sopilnyak.test.service.message.DoCommandMessage;
import oleg.sopilnyak.test.service.message.UndoCommandMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class ActionExecutorTest<T> {
    ActionExecutor actionExecutor;

    @Mock
    Logger logger;

    ActionContext actionContext = ActionContext.builder().actionName("base-test-action").facadeName("base-test-facade").build();
    @Mock
    Context<T> commandContext;
    @Mock
    RootCommand<T> command;

    @BeforeEach
    void setUp() {
        actionExecutor = spy(() -> logger);
        doReturn(command).when(commandContext).getCommand();
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
        Context<Void> context = actionExecutor.rollbackAction(actionContext, (Context<Void>) commandContext);

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

        BaseCommandMessage<T> processed = actionExecutor.processActionCommand(message);

        assertThat(processed).isNotNull().isEqualTo(message);
        verify(command).doCommand(commandContext);
    }

    @Test
    void shouldProcessUndoActionCommand() {
        UndoCommandMessage message = UndoCommandMessage.builder()
                .actionContext(actionContext).context((Context<Void>) commandContext)
                .correlationId(UUID.randomUUID().toString())
                .build();

        BaseCommandMessage<Void> processed = actionExecutor.processActionCommand(message);

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

        Exception e = assertThrows(IllegalArgumentException.class, () -> actionExecutor.processActionCommand(message));

        assertThat(e).isInstanceOf(IllegalArgumentException.class);
        assertThat(e.getMessage()).isEqualTo("Unknown command direction: UNKNOWN");
        verify(command, never()).undoCommand(commandContext);
        verify(command, never()).doCommand(commandContext);
        verify(actionExecutor).getLogger();
        verify(logger).warn("Unknown command '{}' direction: '{}'.", command.getId(), message.getDirection());
    }

    @Test
    void shouldNotProcessActionCommand_NullDirection() {
        BaseCommandMessage<?> message = new BaseCommandMessage<>("correlation-id", actionContext, commandContext) {
            @Override
            public Direction getDirection() {
                return null;
            }
        };

        Exception e = assertThrows(IllegalArgumentException.class, () -> actionExecutor.processActionCommand(message));

        assertThat(e).isInstanceOf(IllegalArgumentException.class);
        assertThat(e.getMessage()).isEqualTo("Command direction is not defined.");
        verify(command, never()).undoCommand(commandContext);
        verify(command, never()).doCommand(commandContext);
        verify(actionExecutor).getLogger();
        verify(logger).warn("Command direction is not defined in message: '{}'.", message);
    }
}
