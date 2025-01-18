package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandContextTest<T> {
    @Mock
    RootCommand<T> rootCommand;
    @Spy
    CommandContext<T> context = CommandContext.<T>builder().build();

    @Test
    void shouldAddStatesInCorrectOrder() {
        List<Context.State> states = List.of(
                // context is built (there is no parameter for redo)
                INIT,
                // context is ready to command redo(...)
                READY,
                // command execution is in progress
                WORK,
                // command redo(...) is finished successfully
                DONE,
                // command execution is finished unsuccessfully
                FAIL,
                // further command execution is canceled
                CANCEL,
                // command undo(...) is finished successfully
                UNDONE
        );

        states.forEach(state -> context.setState(state));

        assertThat(context.getHistory().states()).isEqualTo(states);
        verify(context, times(states.size())).setState(any(Context.State.class));
    }

    @Test
    void shouldAddStateChangedListenerAndReactToStateChanging() {
        AtomicBoolean changed1 = new AtomicBoolean(false);
        AtomicBoolean changed2 = new AtomicBoolean(false);
        Context.StateChangedListener listener1 = (context, previous, newOne) -> changed1.getAndSet(true);
        Context.StateChangedListener listener2 = (context, previous, newOne) -> changed2.getAndSet(true);

        context.addStateListener(listener1);
        context.addStateListener(listener2);

        context.setState(INIT);

        assertThat(changed1.get()).isTrue();
        assertThat(changed2.get()).isTrue();
    }

    @Test
    void shouldSetStartedAtValue() {
        doCallRealMethod().when(rootCommand).createContext();
        Context<T> commandContext = spy(rootCommand.createContext());
        assertThat(commandContext.getCommand()).isEqualTo(rootCommand);
        assertThat(commandContext.getState()).isEqualTo(INIT);
        assertThat(commandContext.getStartedAt()).isNull();
        assertThat(commandContext.getDuration()).isNull();

        if (commandContext instanceof CommandContext<?> cContext) {
            cContext.setRedoParameter(Input.of(-1));
        }
        assertThat(commandContext.getState()).isEqualTo(READY);
        assertThat(commandContext.getStartedAt()).isNull();
        assertThat(commandContext.getDuration()).isNull();

        commandContext.setState(WORK);
        List<Context.State> states = List.of(INIT, READY, WORK);
        assertThat(commandContext.getStartedAt()).isNotNull();
        assertThat(commandContext.getDuration()).isNull();
        assertThat(commandContext.getHistory().states()).isEqualTo(states);
        verify((CommandContext<T>) commandContext).setStartedAt(any(Instant.class));
        sleepMilliseconds(100);
        assertThat(Instant.now().isAfter(commandContext.getStartedAt())).isTrue();
    }

    @Test
    void shouldSetDurationValueStateDone() {
        long sleepTime = 150;
        RootCommand<Boolean> command = mock(RootCommand.class);
        doCallRealMethod().when(command).createContext();
        Context<Boolean> commandContext = spy(command.createContext());
        if (commandContext instanceof CommandContext<?> cContext) {
            cContext.setRedoParameter(Input.of(-1));
        }
        assertThat(commandContext.getState()).isEqualTo(READY);
        commandContext.setState(WORK);
        verify((CommandContext<T>) commandContext).setStartedAt(any(Instant.class));
        assertThat(commandContext.getDuration()).isNull();
        sleepMilliseconds(sleepTime);

        commandContext.setResult(Boolean.FALSE);

        assertThat(commandContext.getState()).isEqualTo(DONE);
        List<Context.State> states = List.of(INIT, READY, WORK, DONE);
        assertThat(commandContext.getHistory().states()).isEqualTo(states);
        assertThat(commandContext.getStartedAt()).isNotNull().isBefore(Instant.now());
        assertThat(commandContext.getDuration()).isNotNull().isGreaterThanOrEqualTo(Duration.ofMillis(sleepTime));
        verify((CommandContext<T>) commandContext).setDuration(any(Duration.class));
    }

    @Test
    void shouldSetDurationValueStateFailed() {
        long sleepTime = 100;
        RootCommand<Boolean> command = mock(RootCommand.class);
        doCallRealMethod().when(command).createContext();
        Context<Boolean> commandContext = spy(command.createContext());
        if (commandContext instanceof CommandContext<?> cContext) {
            cContext.setRedoParameter(Input.of(-1));
        }
        assertThat(commandContext.getState()).isEqualTo(READY);
        commandContext.setState(WORK);
        verify((CommandContext<T>) commandContext).setStartedAt(any(Instant.class));
        assertThat(commandContext.getDuration()).isNull();
        sleepMilliseconds(sleepTime);
        Exception failure = new Exception();

        commandContext.failed(failure);

        assertThat(commandContext.getState()).isEqualTo(FAIL);
        assertThat(commandContext.getResult()).isEmpty();
        assertThat(commandContext.getException()).isSameAs(failure);
        List<Context.State> states = List.of(INIT, READY, WORK, FAIL);
        assertThat(commandContext.getHistory().states()).isEqualTo(states);
        assertThat(commandContext.getStartedAt()).isNotNull().isBefore(Instant.now());
        assertThat(commandContext.getDuration()).isNotNull().isGreaterThanOrEqualTo(Duration.ofMillis(sleepTime));
        verify((CommandContext<T>) commandContext).setDuration(any(Duration.class));
    }

    @Test
    void shouldSetDurationValueStateUndo() {
        long sleepTime = 100;
        RootCommand<Boolean> command = mock(RootCommand.class);
        doCallRealMethod().when(command).createContext();
        Context<Boolean> commandContext = spy(command.createContext());
        if (commandContext instanceof CommandContext<?> cContext) {
            cContext.setRedoParameter(Input.of(-1));
        }
        assertThat(commandContext.getState()).isEqualTo(READY);
        commandContext.setState(WORK);
        verify((CommandContext<T>) commandContext).setStartedAt(any(Instant.class));
        assertThat(commandContext.getDuration()).isNull();
        commandContext.setResult(Boolean.FALSE);

        commandContext.setState(WORK);
        sleepMilliseconds(sleepTime);
        if (commandContext instanceof CommandContext<?> cContext) {
            cContext.setUndoParameter(Input.of(-2));
        }
        commandContext.setState(UNDONE);

        assertThat(commandContext.getState()).isEqualTo(UNDONE);
        assertThat(commandContext.getResult().orElseThrow()).isFalse();
        List<Context.State> states = List.of(INIT, READY, WORK, DONE, WORK, UNDONE);
        assertThat(commandContext.getHistory().states()).isEqualTo(states);
        assertThat(commandContext.getStartedAt()).isNotNull().isBefore(Instant.now());
        assertThat(commandContext.getDuration()).isNotNull().isGreaterThanOrEqualTo(Duration.ofMillis(sleepTime));
        int invokeTimes = 2;
        assertThat(((CommandContext<T>) commandContext).getStartedAtHistory()).hasSize(invokeTimes);
        assertThat(((CommandContext<T>) commandContext).getWorkedHistory()).hasSize(invokeTimes);
        verify((CommandContext<T>) commandContext, times(invokeTimes)).setStartedAt(any(Instant.class));
        verify((CommandContext<T>) commandContext, times(invokeTimes)).setDuration(any(Duration.class));
    }

    /**
    await().atMost(sleepTime, TimeUnit.MILLISECONDS).until(() -> true);
     */
    private static void sleepMilliseconds(long sleepTime) {
        try {
            TimeUnit.MILLISECONDS.sleep(sleepTime);
        } catch (InterruptedException e) {
            // do nothing
        }
    }
}