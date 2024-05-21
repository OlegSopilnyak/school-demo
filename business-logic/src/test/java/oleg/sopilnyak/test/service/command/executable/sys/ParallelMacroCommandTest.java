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
import org.springframework.lang.NonNull;
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
    FakeParallelCommand command;
    @Mock
    SchoolCommand doubleCommand;
    @Mock
    SchoolCommand booleanCommand;
    @Mock
    SchoolCommand intCommand;
    @Mock
    StudentCommand studentCommand;

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
    <T> void shouldDoAllNestedCommands() {
        int parameter = 100;
        allowRealPrepareContextBase(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.isReady()).isTrue());
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        AtomicInteger counter = new AtomicInteger(0);
        Context.StateChangedListener<T> listener = spy(new ContextStateChangedListener<>(counter));
        allowRealNestedCommandExecutionBase();

        command.doNestedCommands(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isEqualTo(command.commands().size());
        // check contexts states
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.isDone()).isTrue());
        checkRegularNestedCommandExecution(doubleCommand, wrapper.getNestedContexts().pop(), listener);
        checkRegularNestedCommandExecution(booleanCommand, wrapper.getNestedContexts().pop(), listener);
        checkRegularNestedCommandExecution(intCommand, wrapper.getNestedContexts().pop(), listener);
    }

    @Test
    void rollbackDoneContexts() {
        assertThat(command).isNotNull();
    }

    // inner classes
    static class FakeParallelCommand extends ParallelMacroCommand {
        private final Logger logger = LoggerFactory.getLogger(FakeParallelCommand.class);

        public FakeParallelCommand(SchedulingTaskExecutor commandContextExecutor) {
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
    private <T> void checkRegularNestedCommandExecution(SchoolCommand nestedCommand,
                                                        @NonNull Context<T> nestedContext,
                                                        Context.StateChangedListener<T> listener) {
        assertThat(nestedContext.isDone()).isTrue();
        verify(nestedCommand).doAsNestedCommand(command, nestedContext, listener);
        verify(command).doNestedCommand(nestedCommand, nestedContext, listener);
        verify(listener).stateChanged(nestedContext, READY, WORK);
        verify(listener).stateChanged(nestedContext, WORK, DONE);
    }
    private void allowRealPrepareContext(SchoolCommand nested, Object parameter) {
        doCallRealMethod().when(nested).createContext(parameter);
        doCallRealMethod().when(nested).acceptPreparedContext(command, parameter);
    }

    private void allowRealPrepareContextBase(Object parameter) {
        allowRealPrepareContext(doubleCommand, parameter);
        allowRealPrepareContext(booleanCommand, parameter);
        allowRealPrepareContext(intCommand, parameter);
    }

    private void allowRealPrepareContextExtra(Object parameter) {
        doCallRealMethod().when(studentCommand).acceptPreparedContext(command, parameter);
    }

    private void allowRealNestedCommandExecution(SchoolCommand nested) {
        doCallRealMethod().when(nested).doAsNestedCommand(eq(command), any(Context.class), any(Context.StateChangedListener.class));
    }

    private void allowRealNestedCommandExecutionBase() {
        allowRealNestedCommandExecution(doubleCommand);
        allowRealNestedCommandExecution(booleanCommand);
        doCallRealMethod().when(intCommand).doAsNestedCommand(eq(command), any(Context.class), any(Context.StateChangedListener.class));
    }

//    private void allowCreateRealContexts() {
//        doCallRealMethod().when(doubleCommand).createContext(any());
//        doCallRealMethod().when(booleanCommand).createContext(any());
//        doCallRealMethod().when(intCommand).createContext(any());
//        // visitor accept
//        doCallRealMethod().when(doubleCommand).acceptPreparedContext(eq(command), any());
//        doCallRealMethod().when(booleanCommand).acceptPreparedContext(eq(command), any());
//        doCallRealMethod().when(intCommand).acceptPreparedContext(eq(command), any());
//    }

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