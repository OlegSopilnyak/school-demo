package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SequentialMacroCommandTest {
    @Spy
    @InjectMocks
    FakeMacroCommand command;
    @Mock
    SchoolCommand doubleCommand;
    @Mock
    SchoolCommand booleanCommand;
    @Mock
    SchoolCommand intCommand;

    @BeforeEach
    void setUp() {
        command.add(doubleCommand);
        command.add(booleanCommand);
        command.add(intCommand);
    }

    @Test
    void checkIntegrity() {
        assertThat(command).isNotNull();
    }

    @Test
    <T> void shouldDoAllNestedRedo() {
        int parameter = 100;
        AtomicInteger counter = new AtomicInteger(0);
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        CommandParameterWrapper<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.getState()).isEqualTo(READY));
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        Context.StateChangedListener<T> listener = spy(new ContextStateChangedListener<>(counter));

        command.doNestedCommands(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isEqualTo(command.commands().size());
        // check contexts states
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.getState()).isEqualTo(DONE));
        Context<T> nestedContext;
        nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.getState()).isEqualTo(DONE);
        verify(command).doNestedCommand(nestedContext, listener);
        verify(command).transferPreviousRedoResult(eq(nestedContext.getCommand()), eq(nestedContext.getResult()), any(Context.class));
        verify(listener).stateChanged(nestedContext, READY, WORK);
        verify(listener).stateChanged(nestedContext, WORK, DONE);
        nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.getState()).isEqualTo(DONE);
        verify(command).doNestedCommand(nestedContext, listener);
        verify(command).transferPreviousRedoResult(eq(nestedContext.getCommand()), eq(nestedContext.getResult()), any(Context.class));
        verify(listener).stateChanged(nestedContext, READY, WORK);
        verify(listener).stateChanged(nestedContext, WORK, DONE);
        nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.getState()).isEqualTo(DONE);
        verify(command).doNestedCommand(nestedContext, listener);
        verify(command, never()).transferPreviousRedoResult(eq(nestedContext.getCommand()), eq(nestedContext.getResult()), any(Context.class));
        verify(listener).stateChanged(nestedContext, READY, WORK);
        verify(listener).stateChanged(nestedContext, WORK, DONE);
    }

    @Test
    <T> void shouldNotDoSomeNestedRedo_NestedRedoThrown() {
        int parameter = 101;
        AtomicInteger counter = new AtomicInteger(0);
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        CommandParameterWrapper<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.getState()).isEqualTo(READY));
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        doThrow(UnableExecuteCommandException.class).when(booleanCommand).doCommand(any(Context.class));
        Context.StateChangedListener<T> listener = spy(new ContextStateChangedListener<>(counter));

        command.doNestedCommands(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isEqualTo(1);
        Context<T> nestedContext;
        nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.getState()).isEqualTo(DONE);
        verify(command).doNestedCommand(nestedContext, listener);
        verify(command).transferPreviousRedoResult(eq(nestedContext.getCommand()), eq(nestedContext.getResult()), any(Context.class));
        verify(listener).stateChanged(nestedContext, READY, WORK);
        verify(listener).stateChanged(nestedContext, WORK, DONE);
        nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.getState()).isEqualTo(FAIL);
        assertThat(nestedContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        verify(command).doNestedCommand(nestedContext, listener);
        verify(command, never()).transferPreviousRedoResult(eq(nestedContext.getCommand()), eq(nestedContext.getResult()), any(Context.class));
        verify(listener).stateChanged(nestedContext, READY, FAIL);
        nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.getState()).isEqualTo(CANCEL);
        verify(command, never()).doNestedCommand(nestedContext, listener);
        verify(command, never()).transferPreviousRedoResult(eq(nestedContext.getCommand()), eq(nestedContext.getResult()), any(Context.class));
        verify(listener).stateChanged(nestedContext, READY, CANCEL);
    }

    @Test
    <T> void shouldNotDoAllNestedRedo_NestedRedoThrown() {
        int parameter = 102;
        AtomicInteger counter = new AtomicInteger(0);
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        CommandParameterWrapper<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.getState()).isEqualTo(READY));
        doThrow(UnableExecuteCommandException.class).when(doubleCommand).doCommand(any(Context.class));
        Context.StateChangedListener<T> listener = spy(new ContextStateChangedListener<>(counter));

        command.doNestedCommands(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isZero();
        Context<T> nestedContext;
        nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.getState()).isEqualTo(FAIL);
        assertThat(nestedContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        verify(command).doNestedCommand(nestedContext, listener);
        verify(listener).stateChanged(nestedContext, READY, FAIL);
        nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.getState()).isEqualTo(CANCEL);
        verify(listener).stateChanged(nestedContext, READY, CANCEL);
        nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.getState()).isEqualTo(CANCEL);
        verify(listener).stateChanged(nestedContext, READY, CANCEL);
    }

    @Test
    <T> void shouldRollbackAllNestedDoneContexts() {
        int parameter = 103;
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        CommandParameterWrapper<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.getState()).isEqualTo(READY));
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        command.doCommand(macroContext);
        Deque<Context<T>> nestedUndoneContexts = macroContext.getUndoParameter();
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);

        Deque<Context<T>> rollbackResults = command.rollbackDoneContexts(nestedUndoneContexts);

        assertThat(nestedUndoneContexts).hasSameSizeAs(rollbackResults);
        int size = nestedUndoneContexts.size();
        List<Context<T>> params = nestedUndoneContexts.stream().toList();
        List<Context<T>> undone = rollbackResults.stream().toList();
        // check revers
        IntStream.range(0, size).forEach(i -> assertThat(undone.get(i)).isEqualTo(params.get(size - i - 1)));
        // check contexts states
        rollbackResults.forEach(ctx -> {
            verify(command).rollbackDoneContext(ctx.getCommand(), ctx);
            assertThat(ctx.getState()).isEqualTo(UNDONE);
        });
    }

    @Test
    <T> void shouldNotRollbackSomeNestedDoneContexts_NestedUndoThrown() {
        int parameter = 104;
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        CommandParameterWrapper<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.getState()).isEqualTo(READY));
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        command.doCommand(macroContext);
        Deque<Context<T>> nestedUndoneContexts = macroContext.getUndoParameter();
        doThrow(UnableExecuteCommandException.class).when(doubleCommand).undoCommand(any(Context.class));
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);

        Deque<Context<T>> rollbackResults = command.rollbackDoneContexts(nestedUndoneContexts);

        assertThat(nestedUndoneContexts).hasSameSizeAs(rollbackResults);
        int size = nestedUndoneContexts.size();
        List<Context<T>> params = nestedUndoneContexts.stream().toList();
        List<Context<T>> undone = rollbackResults.stream().toList();
        // check revers
        IntStream.range(0, size).forEach(i -> assertThat(undone.get(i)).isEqualTo(params.get(size - i - 1)));
        // check contexts order and states
        Context<?> nestedContext;
        nestedContext = rollbackResults.pop();
        verify(command).rollbackDoneContext(intCommand, nestedContext);
        assertThat(nestedContext.getState()).isEqualTo(UNDONE);
        nestedContext = rollbackResults.pop();
        verify(command).rollbackDoneContext(booleanCommand, nestedContext);
        assertThat(nestedContext.getState()).isEqualTo(UNDONE);
        nestedContext = rollbackResults.pop();
        verify(command).rollbackDoneContext(doubleCommand, nestedContext);
        assertThat(nestedContext.getState()).isEqualTo(FAIL);
        assertThat(nestedContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
    }

    static class FakeMacroCommand extends SequentialMacroCommand {
        private final Logger logger = LoggerFactory.getLogger(FakeMacroCommand.class);

        @Override
        public Logger getLog() {
            return logger;
        }

        @Override
        public String getId() {
            return "sequential-fake-command";
        }

        /**
         * To prepare context for particular type of the command
         *
         * @param command   nested command instance
         * @param mainInput macro-command input parameter
         * @return built context of the command for input parameter
         * @see StudentCommand
         * @see StudentCommand#createContext(Object)
         * @see Context
         */
        @Override
        public <T> Context<T> prepareContext(StudentCommand command, Object mainInput) {
            return null;
        }
    }

    static class ContextStateChangedListener<T> implements Context.StateChangedListener<T> {
        private final AtomicInteger counter;

        ContextStateChangedListener(AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        public void stateChanged(Context<T> context, Context.State previous, Context.State newOne) {
            if (newOne == DONE) {
                counter.incrementAndGet();
            }
        }

    }

    // private methods
    private void allowCreateRealContexts(Object parameter) {
        doCallRealMethod().when(doubleCommand).createContext(parameter);
        doCallRealMethod().when(booleanCommand).createContext(parameter);
        doCallRealMethod().when(intCommand).createContext(parameter);
        // visitor accept
        doCallRealMethod().when(doubleCommand).acceptPreparedContext(command, parameter);
        doCallRealMethod().when(booleanCommand).acceptPreparedContext(command, parameter);
        doCallRealMethod().when(intCommand).acceptPreparedContext(command, parameter);
    }

    private <T> void configureNestedRedoResult(SchoolCommand nextedCommand, T result) {
        doAnswer(invocationOnMock -> {
            Context<T> context = invocationOnMock.getArgument(0, Context.class);
            context.setState(WORK);
            context.setResult(result);
            return null;
        }).when(nextedCommand).doCommand(any(Context.class));
    }

    private <T> void configureNestedUndoStatus(SchoolCommand nextedCommand) {
        doAnswer(invocationOnMock -> {
            Context<T> context = invocationOnMock.getArgument(0, Context.class);
            context.setState(WORK);
            context.setState(UNDONE);
            return null;
        }).when(nextedCommand).undoCommand(any(Context.class));
    }
}