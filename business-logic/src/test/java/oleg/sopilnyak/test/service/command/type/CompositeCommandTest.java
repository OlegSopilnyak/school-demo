package oleg.sopilnyak.test.service.command.type;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Deque;
import java.util.List;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.base.CompositeCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;

@ExtendWith(MockitoExtension.class)
class CompositeCommandTest {

    @Mock
    CompositeCommand compositeCommand;

    @BeforeEach
    void setUp() {
        ActionContext.setup("test-facade", "test-action");
    }

    @Test
    void shouldCreateContext() {
        Input<Void> input = Input.empty();
        NestedCommand<Void> firstCommand = mock(NestedCommand.class);
        Context<?> firstContext = mock(Context.class);
        doReturn(firstContext).when(firstCommand).acceptPreparedContext(compositeCommand, input);
        NestedCommand<Void> secondCommand = mock(NestedCommand.class);
        Context<?> secondContext = mock(Context.class);
        doReturn(secondContext).when(secondCommand).acceptPreparedContext(compositeCommand, input);
        NestedCommand<Void> thirdCommand = mock(NestedCommand.class);
        Context<?> thirdContext = mock(Context.class);
        doReturn(thirdContext).when(thirdCommand).acceptPreparedContext(compositeCommand, input);
        doReturn(List.of(firstCommand, secondCommand, thirdCommand)).when(compositeCommand).fromNest();
        doCallRealMethod().when(compositeCommand).createContext(any(Input.class));

        Context<Void> context = compositeCommand.createContext(input);

        // check response
        assertThat(context).isNotNull();
        assertThat(context.isReady()).isTrue();
        Input<?> redo = context.getRedoParameter();
        assertThat(redo).isNotNull().isInstanceOf(MacroCommandParameter.class);
        MacroCommandParameter macro = (MacroCommandParameter) redo;
        assertThat(macro.getRootInput()).isEqualTo(input);
        assertThat(macro.getNestedContexts()).hasSameSizeAs(compositeCommand.fromNest());
        Deque<Context<?>> nested = macro.getNestedContexts();
        assertThat(nested.pop()).isEqualTo(firstContext);
        assertThat(nested.pop()).isEqualTo(secondContext);
        assertThat(nested.pop()).isEqualTo(thirdContext);
        assertThat(nested).isEmpty();
    }

    @Test
    void shouldNotCreateContext_FailedBuiltContext() {
        Input<Void> input = Input.empty();
        NestedCommand<Void> firstCommand = mock(NestedCommand.class);
        Context<?> firstContext = mock(Context.class);
        doReturn(firstContext).when(firstCommand).acceptPreparedContext(compositeCommand, input);
        NestedCommand<Void> secondCommand = mock(NestedCommand.class);
        Context<?> secondContext = mock(Context.class);
        doReturn(secondContext).when(secondCommand).acceptPreparedContext(compositeCommand, input);
        NestedCommand<Void> thirdCommand = mock(NestedCommand.class);
        Context<?> thirdContext = mock(Context.class);
        doReturn(thirdContext).when(thirdCommand).acceptPreparedContext(compositeCommand, input);
        doReturn(List.of(firstCommand, secondCommand, thirdCommand)).when(compositeCommand).fromNest();
        doCallRealMethod().when(compositeCommand).createContext(any(Input.class));
        doCallRealMethod().when(compositeCommand).createContext();
        // third context is failed
        Exception exception = new RuntimeException("Failed");
        doReturn(exception).when(thirdContext).getException();
        doReturn(true).when(thirdContext).isFailed();

        Context<Void> context = compositeCommand.createContext(input);

        // check response
        assertThat(context).isNotNull();
        assertThat(context.isReady()).isFalse();
        assertThat(context.getRedoParameter().isEmpty()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);
        assertThat(context.getException()).isEqualTo(thirdContext.getException());
    }

    @Test
    void shouldNotCreateContext_FirstNestedContextBuildingThrows() {
        Logger logger = mock(Logger.class);
        doReturn(logger).when(compositeCommand).getLog();
        Input<Void> input = Input.empty();
        NestedCommand<Void> firstCommand = mock(NestedCommand.class);
        Context<?> firstContext = mock(Context.class);
        NestedCommand<Void> secondCommand = mock(NestedCommand.class);
        Context<?> secondContext = mock(Context.class);
        doReturn(secondContext).when(secondCommand).acceptPreparedContext(compositeCommand, input);
        NestedCommand<Void> thirdCommand = mock(NestedCommand.class);
        Context<?> thirdContext = mock(Context.class);
        doReturn(thirdContext).when(thirdCommand).acceptPreparedContext(compositeCommand, input);
        doReturn(List.of(firstCommand, secondCommand, thirdCommand)).when(compositeCommand).fromNest();
        doCallRealMethod().when(compositeCommand).createContext(any(Input.class));
        doCallRealMethod().when(compositeCommand).createContext();
        // first context building throws
        Exception exception = new RuntimeException("Failed");
        doReturn(exception).when(firstContext).getException();
        doReturn(true).when(firstContext).isFailed();
        doThrow(exception).when(firstCommand).acceptPreparedContext(compositeCommand, input);
        doReturn(firstContext).when(firstCommand).createFailedContext(exception);

        Context<Void> context = compositeCommand.createContext(input);

        // check response
        assertThat(context).isNotNull();
        assertThat(context.isReady()).isFalse();
        assertThat(context.getRedoParameter().isEmpty()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);
        assertThat(context.getException()).isEqualTo(firstContext.getException());
        // check the behavior
        verify(logger).error(anyString(), eq(firstCommand), eq(input), eq(exception));
        verify(firstCommand).createFailedContext(exception);
    }

    @Test
    void shouldNotCreateContext_LastNestedContextBuildingThrows() {
        Logger logger = mock(Logger.class);
        doReturn(logger).when(compositeCommand).getLog();
        Input<Void> input = Input.empty();
        NestedCommand<Void> firstCommand = mock(NestedCommand.class);
        Context<?> firstContext = mock(Context.class);
        doReturn(firstContext).when(firstCommand).acceptPreparedContext(compositeCommand, input);
        NestedCommand<Void> secondCommand = mock(NestedCommand.class);
        Context<?> secondContext = mock(Context.class);
        doReturn(secondContext).when(secondCommand).acceptPreparedContext(compositeCommand, input);
        NestedCommand<Void> thirdCommand = mock(NestedCommand.class);
        Context<?> thirdContext = mock(Context.class);
        doReturn(List.of(firstCommand, secondCommand, thirdCommand)).when(compositeCommand).fromNest();
        doCallRealMethod().when(compositeCommand).createContext(any(Input.class));
        doCallRealMethod().when(compositeCommand).createContext();
        // third context building throws
        Exception exception = new RuntimeException("Failed");
        doReturn(exception).when(thirdContext).getException();
        doReturn(true).when(thirdContext).isFailed();
        doThrow(exception).when(thirdCommand).acceptPreparedContext(compositeCommand, input);
        doReturn(thirdContext).when(thirdCommand).createFailedContext(exception);

        Context<Void> context = compositeCommand.createContext(input);

        // check response
        assertThat(context).isNotNull();
        assertThat(context.isReady()).isFalse();
        assertThat(context.getRedoParameter().isEmpty()).isTrue();
        assertThat(context.getException()).isEqualTo(exception);
        assertThat(context.getException()).isEqualTo(thirdContext.getException());
        // check the behavior
        verify(logger).error(anyString(), eq(thirdCommand), eq(input), eq(exception));
        verify(thirdCommand).createFailedContext(exception);
    }

    @Test
    void shouldAcceptPreparedContext() {
        Input<Void> input = Input.empty();
        doCallRealMethod().when(compositeCommand).acceptPreparedContext(compositeCommand, input);

        compositeCommand.acceptPreparedContext(compositeCommand, input);

        verify(compositeCommand).prepareContext(compositeCommand, input);
    }

    @Test
    void shouldNotAcceptPreparedContext_PrepareContextThrows() {
        Input<Void> input = Input.empty();
        doCallRealMethod().when(compositeCommand).acceptPreparedContext(compositeCommand, input);
        Exception exception = new RuntimeException("Failed");
        doThrow(exception).when(compositeCommand).prepareContext(compositeCommand, input);

        var error = assertThrows(Exception.class, () -> compositeCommand.acceptPreparedContext(compositeCommand, input));

        assertThat(error).isEqualTo(exception);
        verify(compositeCommand).prepareContext(compositeCommand, input);
    }

    @Test
    void shouldExecuteDoNested_NoStateChangeListener() {
        ActionContext actionContext = ActionContext.current();
        ActionExecutor executor = mock(ActionExecutor.class);
        doReturn(executor).when(compositeCommand).getActionExecutor();
        Context<?> nestedContext = mock(Context.class);
        doReturn(nestedContext).when(executor).commitAction(actionContext, nestedContext);
        doCallRealMethod().when(compositeCommand).executeDoNested(nestedContext);
        doCallRealMethod().when(compositeCommand).executeDoNested(eq(nestedContext), any());

        Context<?> result = compositeCommand.executeDoNested(nestedContext);

        assertThat(result).isNotNull().isEqualTo(nestedContext);
        verify(compositeCommand).executeDoNested(eq(nestedContext), any());
        verify(executor).commitAction(actionContext, nestedContext);
    }

    @Test
    void shouldExecuteDoNested_WithStateChangeListener() {
        ActionContext actionContext = ActionContext.current();
        RootCommand<?> nestedCommand = mock(RootCommand.class);
        Input<Void> input = Input.empty();
        doCallRealMethod().when(nestedCommand).createContext(input);
        Context<?> nestedContext = nestedCommand.createContext(input);
        assertThat(nestedContext.isReady()).isTrue();
        Context.StateChangedListener listener = mock(Context.StateChangedListener.class);
        ActionExecutor executor = mock(ActionExecutor.class);
        doReturn(executor).when(compositeCommand).getActionExecutor();
        doAnswer((Answer<Context<Void>>) invocation -> {
            Context<Void> context = invocation.getArgument(1, Context.class);
            context.setState(Context.State.WORK);
            context.setState(Context.State.DONE);
            return context;
        }).when(executor).commitAction(actionContext, nestedContext);
        doCallRealMethod().when(compositeCommand).executeDoNested(eq(nestedContext), any(Context.StateChangedListener.class));

        Context<?> result = compositeCommand.executeDoNested(nestedContext, listener);

        // check response
        assertThat(result).isNotNull().isEqualTo(nestedContext);
        assertThat(result.isDone()).isTrue();
        // check the behavior
        verify(compositeCommand).executeDoNested(nestedContext, listener);
        verify(executor).commitAction(actionContext, nestedContext);
        InOrder inOrder = Mockito.inOrder(listener);
        inOrder.verify(listener).stateChanged(nestedContext, Context.State.READY, Context.State.WORK);
        inOrder.verify(listener).stateChanged(nestedContext, Context.State.WORK, Context.State.DONE);
    }


    @Test
    void shouldExecuteUndoNested() {
        ActionContext actionContext = ActionContext.current();
        ActionExecutor executor = mock(ActionExecutor.class);
        doReturn(executor).when(compositeCommand).getActionExecutor();
        Context<Void> nestedContext = mock(Context.class);
        doReturn(nestedContext).when(executor).rollbackAction(actionContext, nestedContext);
        doCallRealMethod().when(compositeCommand).executeUndoNested(nestedContext);

        Context<?> result = compositeCommand.executeUndoNested(nestedContext);

        // check response
        assertThat(result).isNotNull().isEqualTo(nestedContext);
        // check the behavior
        verify(executor).rollbackAction(actionContext, nestedContext);
    }
}