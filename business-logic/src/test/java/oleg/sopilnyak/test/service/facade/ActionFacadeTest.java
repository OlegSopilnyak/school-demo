package oleg.sopilnyak.test.service.facade;

import static oleg.sopilnyak.test.service.facade.ActionFacade.throwFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.factory.base.CommandsFactory;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.exception.CommandNotRegisteredInFactoryException;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;

import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class ActionFacadeTest {

    @Mock
    CommandsFactory<?> commandsFactory;
    @Mock
    Input<?> input;
    @Mock
    Context<?> context;

    @Mock
    Logger log;
    @Mock
    ActionExecutor actionExecutor;

    ActionFacade actionFacade;

    @BeforeEach
    void setUp() {
        ActionContext.setup("test", "test");
        actionFacade = spy(new ActionFacade() {
            @Override
            public Logger getLogger() {
                return log; // Mocked logger
            }

            @Override
            public ActionExecutor getActionExecutor() {
                return actionExecutor; // Mocked executor
            }
        });
    }

    @Test
    void shouldExecuteCommandWithDefaultErrorHandler() {
        String commandId = "1";
        ActionContext currentActionContext = ActionContext.current();
        doReturn(true).when(context).isDone();
        doReturn(Optional.of(Boolean.TRUE)).when(context).getResult();
        doReturn(context).when(commandsFactory).makeCommandContext(commandId, input);
        doReturn(context).when(actionExecutor).commitAction(ActionContext.current(), context);

        Optional<Boolean> result = actionFacade.executeCommand(commandId, commandsFactory, input);

        assertThat(result).isNotNull();
        assertThat(result.get()).isTrue();
        verify(actionFacade).executeCommand(eq(commandId), eq(commandsFactory), eq(input), any(Consumer.class));
        verify(actionExecutor).commitAction(currentActionContext, context);
    }

    @Test
    void shouldNotExecuteCommandWithDefaultErrorHandler_CannotMakeContext() {
        String commandId = "2";

        var result = assertThrows(Exception.class, () -> actionFacade.executeCommand(commandId, commandsFactory, input));

        assertThat(result).isNotNull().isInstanceOf(UnableExecuteCommandException.class);
        assertThat(result.getMessage()).startsWith("Cannot execute command '" + commandId);
        assertThat(result.getCause()).isInstanceOf(CommandNotRegisteredInFactoryException.class);
        assertThat(result.getCause().getMessage()).startsWith("Command '" + commandId + "' is not registered in factory :");
        verify(actionFacade).executeCommand(eq(commandId), eq(commandsFactory), eq(input), any(Consumer.class));
        verify(actionExecutor, never()).commitAction(any(ActionContext.class), any(Context.class));
    }

    @Test
    void shouldNotExecuteCommandWithDefaultErrorHandler_CommandExecutionFailed() {
        String commandId = "3";
        ActionContext currentActionContext = ActionContext.current();
        doReturn(false).when(context).isDone();
        doReturn(new RuntimeException()).when(context).getException();
        doReturn(context).when(commandsFactory).makeCommandContext(commandId, input);
        doReturn(context).when(actionExecutor).commitAction(ActionContext.current(), context);

        var result = assertThrows(Exception.class, () -> actionFacade.executeCommand(commandId, commandsFactory, input));

        assertThat(result).isNotNull().isInstanceOf(UnableExecuteCommandException.class);
        assertThat(result.getMessage()).startsWith("Cannot execute command '" + commandId);
        assertThat(result.getCause()).isInstanceOf(RuntimeException.class);
        verify(actionFacade).executeCommand(eq(commandId), eq(commandsFactory), eq(input), any(Consumer.class));
        verify(actionExecutor).commitAction(currentActionContext, context);
    }

    @Test
    void shouldExecuteCommandWithCustomErrorHandler() {
        String commandId = "4";
        ActionContext currentActionContext = ActionContext.current();
        doReturn(true).when(context).isDone();
        doReturn(Optional.of(Boolean.TRUE)).when(context).getResult();
        doReturn(context).when(commandsFactory).makeCommandContext(commandId, input);
        doReturn(context).when(actionExecutor).commitAction(ActionContext.current(), context);

        Optional<Boolean> result = actionFacade.executeCommand(commandId, commandsFactory, input, this::justLogException);

        assertThat(result).isNotNull();
        assertThat(result.get()).isNotNull().isTrue();
        verify(actionExecutor).commitAction(currentActionContext, context);
        verify(log, never()).error(anyString());
    }

    @Test
    void shouldNotExecuteCommandWithCustomErrorHandler_CannotMakeContext() {
        String commandId = "5";

        var result = assertThrows(Exception.class, () -> actionFacade.executeCommand(commandId, commandsFactory, input, this::justLogException));

        assertThat(result).isNotNull().isInstanceOf(UnableExecuteCommandException.class);
        assertThat(result.getMessage()).startsWith("Cannot execute command '" + commandId);
        assertThat(result.getCause()).isInstanceOf(CommandNotRegisteredInFactoryException.class);
        assertThat(result.getCause().getMessage()).startsWith("Command '" + commandId + "' is not registered in factory :");
        verify(actionExecutor, never()).commitAction(any(ActionContext.class), any(Context.class));
        verify(log, never()).error(anyString());
    }

    @Test
    void shouldNotExecuteCommandWithCustomErrorHandler_CommandExecutionFailed() {
        String commandId = "6";
        ActionContext currentActionContext = ActionContext.current();
        doReturn(false).when(context).isDone();
        doReturn(new RuntimeException("Failed execution of " + commandId)).when(context).getException();
        doReturn(context).when(commandsFactory).makeCommandContext(commandId, input);
        doReturn(context).when(actionExecutor).commitAction(ActionContext.current(), context);
        Consumer<Exception> customErrorHandler = exception -> {
            justLogException(exception);
            throwFor(commandId, exception);
        };

        var result = assertThrows(Exception.class, () -> actionFacade.executeCommand(commandId, commandsFactory, input, customErrorHandler));

        assertThat(result).isNotNull().isInstanceOf(UnableExecuteCommandException.class);
        assertThat(result.getMessage()).startsWith("Cannot execute command '" + commandId);
        assertThat(result.getCause()).isInstanceOf(RuntimeException.class);
        verify(actionExecutor).commitAction(currentActionContext, context);
        verify(log).error("Custom error handler: {}", "Failed execution of " + commandId);
    }

    // private methods
    private void justLogException(Exception exception) {
        log.error("Custom error handler: {}", exception.getMessage());
    }

}
