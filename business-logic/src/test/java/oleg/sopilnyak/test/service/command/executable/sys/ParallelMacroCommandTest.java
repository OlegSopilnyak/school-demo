package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
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

import java.util.Deque;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static oleg.sopilnyak.test.service.command.executable.sys.ParallelMacroCommandTest.FakeParallelCommand.overridedStudentContext;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParallelMacroCommandTest {

    ThreadPoolTaskExecutor executor = spy(new ThreadPoolTaskExecutor());
    @Spy
    @InjectMocks
    volatile FakeParallelCommand command;
    @Mock
    RootCommand<?> doubleCommand;
    @Mock
    RootCommand<?> booleanCommand;
    @Mock
    RootCommand<?> intCommand;
    @Mock
    StudentCommand<Double> studentCommand;

    @BeforeEach
    void setUp() {
        executor.initialize();
        command.putToNest(doubleCommand);
        command.putToNest(booleanCommand);
        command.putToNest(intCommand);
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
    void shouldDoAllNestedCommands() {
        int parameter = 100;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContextBase(inputParameter);
        Context<?> macroContext = command.createContext(inputParameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.isReady()).isTrue());
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        AtomicInteger counter = new AtomicInteger(0);
        Context.StateChangedListener listener = spy(new ContextStateChangedListener(counter));
        allowRealNestedCommandExecutionBase();

        command.executeNested(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isEqualTo(command.fromNest().size());
        verify(executor, times(counter.get())).submit(any(Callable.class));
        // check contexts states
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.isDone()).isTrue());
        checkRegularNestedCommandExecution(doubleCommand, wrapper.getNestedContexts().pop(), listener);
        checkRegularNestedCommandExecution(booleanCommand, wrapper.getNestedContexts().pop(), listener);
        checkRegularNestedCommandExecution(intCommand, wrapper.getNestedContexts().pop(), listener);
    }

    @Test
    <T> void shouldDoParallelCommand_BaseCommands() {
        int parameter = 101;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        verifyNestedCommandContextPreparation(command, doubleCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, booleanCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, intCommand, inputParameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10.0);
        allowRealNestedCommandExecutionBase();

        // doing command with macro-context
        command.doCommand(macroContext);

        // after do check
        verify(command).executeDo(macroContext);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(macroContext.isDone()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(macroContext.<Deque<Context<?>>>getUndoParameter().value()).hasSameSizeAs(wrapper.getNestedContexts());
        wrapper.getNestedContexts().forEach(context -> {
            Context<T> nestedContext = (Context<T>) context;
            RootCommand<T> nestedCommand = nestedContext.getCommand();
            verify(nestedCommand).doAsNestedCommand(eq(command), eq(context), any(Context.StateChangedListener.class));
            verify(command).doNestedCommand(eq(nestedCommand), eq(nestedContext), any(Context.StateChangedListener.class));
            verify(nestedCommand).doCommand(nestedContext);
        });
        macroContext.<Deque<Context<?>>>getUndoParameter().value().forEach(context -> assertThat(context.isDone()).isTrue());
        verify(executor, times(command.fromNest().size())).submit(any(Callable.class));
    }

    @Test
    <T> void shouldDoParallelCommand_ExtraCommands() {
        int parameter = 102;
        Input<Integer> inputParameter = Input.of(parameter);
        command = spy(new FakeParallelCommand(executor, studentCommand));
        command.putToNest(studentCommand);
        command.putToNest(doubleCommand);
        command.putToNest(booleanCommand);
        command.putToNest(intCommand);
        allowRealPrepareContextBase(inputParameter);
        allowRealPrepareContextExtra(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.isReady()).isTrue());
        assertThat(overridedStudentContext).isEqualTo(wrapper.getNestedContexts().getFirst());
        verifyNestedCommandContextPreparation(command, doubleCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, booleanCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, intCommand, inputParameter);
        verify(studentCommand).acceptPreparedContext(command, inputParameter);
        verify(command).prepareContext(studentCommand, inputParameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10.0);
        allowRealNestedCommandExecutionBase();
        allowRealNestedCommandExecutionExtra();

        // doing command with macro-context
        command.doCommand(macroContext);

        // after do check
        verify(command).executeDo(macroContext);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(macroContext.isDone()).isTrue();
        assertThat(macroContext.<Deque<Context<T>>>getUndoParameter().value()).hasSameSizeAs(wrapper.getNestedContexts());
        wrapper.getNestedContexts().stream()
                .filter(context -> context != overridedStudentContext)
                .forEach(context -> {
                    Context<T> nestedContext = (Context<T>) context;
                    RootCommand<T> nestedCommand = nestedContext.getCommand();
                    verify(nestedCommand).doAsNestedCommand(eq(command), eq(context), any(Context.StateChangedListener.class));
                    verify(command).doNestedCommand(eq(nestedCommand), eq(nestedContext), any(Context.StateChangedListener.class));
                    verify(nestedCommand).doCommand(nestedContext);
                });
        verify(studentCommand).doAsNestedCommand(eq(command), eq(overridedStudentContext), any(Context.StateChangedListener.class));
        verify(command).doNestedCommand(any(StudentCommand.class), eq(overridedStudentContext), any(Context.StateChangedListener.class));
        verify(studentCommand, never()).doCommand(any(Context.class));
        macroContext.<Deque<Context<T>>>getUndoParameter().value().forEach(context -> assertThat(context.isDone()).isTrue());
        verify(executor, times(command.fromNest().size())).submit(any(Callable.class));
    }

    @Test
    <T> void shouldNotDoParallelCommand_doCommandThrowsException() {
        int parameter = 101;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        verifyNestedCommandContextPreparation(command, doubleCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, booleanCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, intCommand, inputParameter);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        allowRealNestedCommandExecutionBase();
        doThrow(UnableExecuteCommandException.class).when(doubleCommand).doCommand(any(Context.class));

        // doing command with macro-context
        command.doCommand(macroContext);

        // after do check
        verify(command).executeDo(macroContext);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(macroContext.isFailed()).isTrue();
        assertThat(macroContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        assertThat(macroContext.<Deque<Context<T>>>getUndoParameter().isEmpty()).isTrue();

        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        wrapper.getNestedContexts().stream()
                .filter(context -> context.getCommand() != doubleCommand)
                .forEach(context -> {
                    Context<T> nestedContext = (Context<T>) context;
                    RootCommand<T> nestedCommand = nestedContext.getCommand();
                    verify(nestedCommand).doAsNestedCommand(eq(command), eq(context), any(Context.StateChangedListener.class));
                    verify(command).doNestedCommand(eq(nestedCommand), eq(nestedContext), any(Context.StateChangedListener.class));
                    verify(nestedCommand).doCommand(nestedContext);
                    assertThat(context.isDone()).isTrue();
                    verify(nestedCommand).undoAsNestedCommand(command, context);
                });
        Context<T> doubleContext = (Context<T>) wrapper.getNestedContexts().getFirst();
        assertThat(doubleContext.isFailed()).isTrue();
        assertThat(doubleContext.getCommand()).isSameAs(doubleCommand);
        verify(doubleContext.getCommand()).doCommand(doubleContext);
        verify(doubleCommand, never()).undoAsNestedCommand(eq(command), any(Context.class));
        int submitted = command.fromNest().size() * 2 - 1;
        verify(executor, times(submitted)).submit(any(Callable.class));
    }

    @Test
    void shouldRollbackAllNestedDoneContexts_BaseCommands() {
        int parameter = 103;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.isReady()).isTrue();
        assertThat(macroContext.getCommand()).isEqualTo(command);
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.isReady()).isTrue());
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10.0);
        allowRealNestedCommandExecutionBase();
        command.doCommand(macroContext);
        Deque<Context<?>> nestedUndoneContexts = macroContext.<Deque<Context<?>>>getUndoParameter().value();
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);
        allowRealNestedCommandRollbackBase();
        reset(executor);

        Deque<Context<?>> rollbackResults = command.rollbackNestedDone(Input.of(nestedUndoneContexts));

        assertThat(nestedUndoneContexts).hasSameSizeAs(rollbackResults);
        // check contexts states
        rollbackResults.forEach(context -> {
            verify(context.getCommand()).undoAsNestedCommand(command, context);
            verify(command).undoNestedCommand(context.getCommand(), context);
            assertThat(context.getState()).isEqualTo(UNDONE);
        });
        verify(executor, times(rollbackResults.size())).submit(any(Callable.class));
    }

    @Test
    void shouldUndoParallelCommand_BaseCommands() {
        int parameter = 104;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        verifyNestedCommandContextPreparation(command, doubleCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, booleanCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, intCommand, inputParameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10.0);
        allowRealNestedCommandExecutionBase();
        command.doCommand(macroContext);
        assertThat(macroContext.isDone()).isTrue();
        Deque<Context<?>> nestedDoneContexts = macroContext.<Deque<Context<?>>>getUndoParameter().value();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.getState()).isEqualTo(DONE));
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);
        allowRealNestedCommandRollbackBase();
        reset(executor);

        command.undoCommand(macroContext);

        assertThat(macroContext.getState()).isEqualTo(UNDONE);
        nestedDoneContexts.forEach(context -> assertThat(context.getState()).isEqualTo(UNDONE));

        verify(command).executeUndo(macroContext);
        verify(command).rollbackNestedDone(any(Input.class));
        nestedDoneContexts.forEach(context -> {
            final var nestedCommand = context.getCommand();
            verify(nestedCommand).undoAsNestedCommand(command, context);
            verify(command).undoNestedCommand(nestedCommand, context);
            verify(nestedCommand).undoCommand(context);
        });
        verify(executor, times(nestedDoneContexts.size())).submit(any(Callable.class));
    }

    @Test
    void shouldUndoParallelCommand_ExtraCommands() {
        int parameter = 105;
        Input<Integer> inputParameter = Input.of(parameter);
        command = spy(new FakeParallelCommand(executor, studentCommand));
        command.putToNest(studentCommand);
        command.putToNest(doubleCommand);
        command.putToNest(booleanCommand);
        command.putToNest(intCommand);
        allowRealPrepareContextBase(inputParameter);
        allowRealPrepareContextExtra(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.isReady()).isTrue());
        assertThat(overridedStudentContext).isEqualTo(wrapper.getNestedContexts().getFirst());
        verifyNestedCommandContextPreparation(command, doubleCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, booleanCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, intCommand, inputParameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10.0);
        allowRealNestedCommandExecutionBase();
        allowRealNestedCommandExecutionExtra();
        command.doCommand(macroContext);
        assertThat(macroContext.isDone()).isTrue();
        Deque<Context<?>> nestedDoneContexts = macroContext.<Deque<Context<?>>>getUndoParameter().value();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.isDone()).isTrue());
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);
        allowRealNestedCommandRollbackBase();
        allowRealNestedCommandRollbackExtra();
        reset(executor);

        command.undoCommand(macroContext);

        assertThat(macroContext.getState()).isEqualTo(UNDONE);
        nestedDoneContexts.stream()
                .filter(context -> context != overridedStudentContext)
                .forEach(context -> assertThat(context.getState()).isEqualTo(UNDONE));
        nestedDoneContexts.stream()
                .filter(context -> context != overridedStudentContext)
                .forEach(context -> {
                    final var nestedCommand = context.getCommand();
                    verify(nestedCommand).undoAsNestedCommand(command, context);
                    verify(command).undoNestedCommand(nestedCommand, context);
                    verify(nestedCommand).undoCommand(context);
                });
        assertThat(overridedStudentContext.getState()).isEqualTo(CANCEL);
        verify(executor, times(nestedDoneContexts.size())).submit(any(Callable.class));
        verify(command).executeUndo(macroContext);
        verify(command).rollbackNestedDone(macroContext.getUndoParameter());
    }

    @Test
    void shouldNotUndoParallelCommand_undoCommandThrowsException() {
        int parameter = 104;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        verifyNestedCommandContextPreparation(command, doubleCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, booleanCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, intCommand, inputParameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10.0);
        allowRealNestedCommandExecutionBase();
        command.doCommand(macroContext);
        assertThat(macroContext.isDone()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedDoneContexts = macroContext.<Deque<Context<?>>>getUndoParameter().value();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.isDone()).isTrue());
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);
        allowRealNestedCommandRollbackBase();
        doThrow(UnableExecuteCommandException.class).when(doubleCommand).undoCommand(any(Context.class));
        reset(executor);

        command.undoCommand(macroContext);

        assertThat(macroContext.isFailed()).isTrue();
        nestedDoneContexts.stream()
                .filter(context -> context.getCommand() != doubleCommand)
                .forEach(context -> assertThat(context.getState()).isEqualTo(DONE));
        Context<?> doubleContext = wrapper.getNestedContexts().getFirst();
        assertThat(doubleContext.isFailed()).isTrue();
        assertThat(macroContext.getException()).isEqualTo(doubleContext.getException());
        verify(command).executeUndo(macroContext);
        verify(command).rollbackNestedDone(any(Input.class));
        nestedDoneContexts.forEach(context -> {
            final var nestedCommand = context.getCommand();
            verify(nestedCommand).undoAsNestedCommand(command, context);
            verify(command).undoNestedCommand(nestedCommand, context);
            verify(nestedCommand).undoCommand(context);
        });
        verify(executor, times(nestedDoneContexts.size())).submit(any(Callable.class));
    }

    //    // inner classes
    static class FakeParallelCommand extends ParallelMacroCommand<Double> {
        static CommandContext<Double> overridedStudentContext;
        private final Logger logger = LoggerFactory.getLogger(FakeParallelCommand.class);
        private final SchedulingTaskExecutor commandContextExecutor;

        public FakeParallelCommand(SchedulingTaskExecutor commandContextExecutor, StudentCommand<Double> student) {
            this.commandContextExecutor = commandContextExecutor;
            overridedStudentContext = CommandContext.<Double>builder().command(student).state(INIT).build();
        }

        @Override
        public <T> Context<T> prepareContext(StudentCommand<T> command, Input<?> mainInput) {
            overridedStudentContext.setRedoParameter(Input.of(200));
            return (Context<T>) overridedStudentContext;
        }

        @Override
        public <N> void doNestedCommand(StudentCommand<N> command, Context<N> doContext, Context.StateChangedListener stateListener) {
            doContext.addStateListener(stateListener);
            doContext.setState(WORK);
            ((Context<Double>) doContext).setResult(100.0);
            doContext.removeStateListener(stateListener);
        }

        @Override
        public Context<?> undoNestedCommand(StudentCommand<?> command, Context<?> undoContext) {
            undoContext.setState(CANCEL);
            return undoContext;
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
         * To get access to command's command-context executor
         *
         * @return instance of executor
         */
        @Override
        public SchedulingTaskExecutor getExecutor() {
            return commandContextExecutor;
        }
    }

    static class ContextStateChangedListener implements Context.StateChangedListener {
        private final AtomicInteger counter;

        ContextStateChangedListener(AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        public void stateChanged(Context<?> context, Context.State previous, Context.State current) {
            if (current == DONE) {
                counter.incrementAndGet();
            }
        }

    }

    // private methods
    private <N> void checkRegularNestedCommandExecution(RootCommand<?> nestedCommand, Context<N> nestedContext,
                                                        Context.StateChangedListener listener) {
        assertThat(nestedContext.isDone()).isTrue();
        verify(nestedCommand).doAsNestedCommand(command, nestedContext, listener);
        verify(command).doNestedCommand((RootCommand<N>) nestedCommand, nestedContext, listener);
        verify(listener).stateChanged(nestedContext, READY, WORK);
        verify(listener).stateChanged(nestedContext, WORK, DONE);
    }

    private void allowRealPrepareContext(RootCommand<?> nested, Input<?> parameter) {
        doCallRealMethod().when(nested).createContext(parameter);
        doCallRealMethod().when(nested).acceptPreparedContext(command, parameter);
    }

    private void allowRealPrepareContextBase(Input<?> parameter) {
        allowRealPrepareContext(doubleCommand, parameter);
        allowRealPrepareContext(booleanCommand, parameter);
        allowRealPrepareContext(intCommand, parameter);
    }

    private void allowRealPrepareContextExtra(Input<?> parameter) {
        doCallRealMethod().when(studentCommand).acceptPreparedContext(command, parameter);
    }

    private void allowRealNestedCommandExecution(RootCommand<?> nested) {
        doCallRealMethod().when(nested).doAsNestedCommand(eq(command), any(Context.class), any(Context.StateChangedListener.class));
    }

    private void allowRealNestedCommandExecutionBase() {
        allowRealNestedCommandExecution(doubleCommand);
        allowRealNestedCommandExecution(booleanCommand);
        doCallRealMethod().when(intCommand).doAsNestedCommand(eq(command), any(Context.class), any(Context.StateChangedListener.class));
    }

    private void allowRealNestedCommandExecutionExtra() {
        allowRealNestedCommandExecution(studentCommand);
    }

    private void allowRealNestedCommandRollback(RootCommand<?> nested) {
        doCallRealMethod().when(nested).undoAsNestedCommand(eq(command), any(Context.class));
    }

    private void allowRealNestedCommandRollbackBase() {
        allowRealNestedCommandRollback(doubleCommand);
        allowRealNestedCommandRollback(booleanCommand);
        allowRealNestedCommandRollback(intCommand);
    }

    private void allowRealNestedCommandRollbackExtra() {
        allowRealNestedCommandRollback(studentCommand);
    }

    private static void verifyNestedCommandContextPreparation(FakeParallelCommand command,
                                                              RootCommand<?> nestedCommand,
                                                              Input<?> parameter) {
        verify(nestedCommand).acceptPreparedContext(command, parameter);
        verify(command).prepareContext(nestedCommand, parameter);
    }


    private <T> void configureNestedRedoResult(RootCommand<?> nextedCommand, T result) {
        doAnswer(invocationOnMock -> {
            Context<T> context = invocationOnMock.getArgument(0, Context.class);
            context.setState(WORK);
            context.setResult(result);
            return null;
        }).when(nextedCommand).doCommand(any(Context.class));
    }

    private <T> void configureNestedUndoStatus(RootCommand<T> nextedCommand) {
        doAnswer(invocationOnMock -> {
            Context<T> context = invocationOnMock.getArgument(0, Context.class);
            context.setState(WORK);
            context.setState(UNDONE);
            return null;
        }).when(nextedCommand).undoCommand(any(Context.class));
    }
}