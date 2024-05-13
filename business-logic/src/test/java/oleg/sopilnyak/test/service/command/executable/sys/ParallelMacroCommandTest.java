package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.SchedulingTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.atomic.AtomicInteger;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParallelMacroCommandTest {

    ThreadPoolTaskExecutor executor = spy(new ThreadPoolTaskExecutor());
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
        executor.initialize();
        command.add(doubleCommand);
        command.add(booleanCommand);
        command.add(intCommand);
    }

    @AfterEach
    void tearDown() {
        reset(executor);
    }

    @Test
    void checkIntegrity() {
        assertThat(command).isNotNull();
    }

    @Test
    <T> void shouldDoAllNestedRedo() {
        int parameter = 100;
        AtomicInteger counter = new AtomicInteger(0);
        allowCreateRealContexts();
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
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.isDone()).isTrue());
        Context<T> nestedContext;
        nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.isDone()).isTrue();
        verify(command).doNestedCommand(nestedContext, listener);
        verify(listener).stateChanged(nestedContext, READY, WORK);
        verify(listener).stateChanged(nestedContext, WORK, DONE);
        nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.isDone()).isTrue();
        verify(command).doNestedCommand(nestedContext, listener);
        verify(listener).stateChanged(nestedContext, READY, WORK);
        verify(listener).stateChanged(nestedContext, WORK, DONE);
        nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.isDone()).isTrue();
        verify(command).doNestedCommand(nestedContext, listener);
        verify(listener).stateChanged(nestedContext, READY, WORK);
        verify(listener).stateChanged(nestedContext, WORK, DONE);
    }

    @Test
    void rollbackDoneContexts() {
    }

    // inner classes
    static class FakeMacroCommand extends ParallelMacroCommand {
        private final Logger logger = LoggerFactory.getLogger(ParallelMacroCommandTest.FakeMacroCommand.class);

        public FakeMacroCommand(SchedulingTaskExecutor commandContextExecutor) {
            super(commandContextExecutor);
        }

        @Override
        public Logger getLog() {
            return logger;
        }

        @Override
        public String getId() {
            return "parallel-fake-command";
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
    private void allowCreateRealContexts() {
        doCallRealMethod().when(doubleCommand).createContext(any());
        doCallRealMethod().when(booleanCommand).createContext(any());
        doCallRealMethod().when(intCommand).createContext(any());
        // visitor accept
        doCallRealMethod().when(doubleCommand).acceptPreparedContext(eq(command), any());
        doCallRealMethod().when(booleanCommand).acceptPreparedContext(eq(command), any());
        doCallRealMethod().when(intCommand).acceptPreparedContext(eq(command), any());
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