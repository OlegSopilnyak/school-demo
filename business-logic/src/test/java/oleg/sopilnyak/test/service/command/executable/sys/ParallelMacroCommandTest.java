package oleg.sopilnyak.test.service.command.executable.sys;

import static oleg.sopilnyak.test.service.command.executable.sys.ParallelMacroCommandTest.FakeParallelCommand.overrideStudentContext;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.CANCEL;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.INIT;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.READY;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.WORK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;
import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;
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

@ExtendWith(MockitoExtension.class)
class ParallelMacroCommandTest {

    ThreadPoolTaskExecutor executor = spy(new ThreadPoolTaskExecutor());
    @Mock
    RootCommand<?> doubleCommand;
    @Mock
    RootCommand<?> booleanCommand;
    @Mock
    RootCommand<?> intCommand;
    @Mock
    StudentCommand<Double> studentCommand;
    @Mock
    ActionExecutor actionExecutor;
    @Spy
    @InjectMocks
    volatile FakeParallelCommand command;

    @BeforeEach
    void setUp() {
        executor.initialize();
        command.putToNest(doubleCommand);
        command.putToNest(booleanCommand);
        command.putToNest(intCommand);
        ActionContext.setup("test-facade", "test-action");
    }

    @AfterEach
    void tearDown() {
        reset(executor);
    }

    @Test
    void checkIntegrity() {
        assertThat(command).isNotNull();
        assertThat(executor).isNotNull();
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
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));

        command.executeNested(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isEqualTo(command.fromNest().size());
        verify(executor, times(counter.get())).execute(any(Runnable.class));
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
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));

        // doing command with macro-context
        command.doCommand(macroContext);

        // after do check
        verify(command).executeDo(macroContext);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(macroContext.isDone()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(macroContext.<Deque<Context<?>>>getUndoParameter().value()).hasSameSizeAs(wrapper.getNestedContexts());
        wrapper.getNestedContexts().forEach(context -> {
            assertThat(context.isDone()).isTrue();
            verify(command).executeDoNested(eq(context), any(Context.StateChangedListener.class));
            Context<T> nestedContext = (Context<T>) context;
            verify(nestedContext.getCommand()).doCommand(nestedContext);
        });
        macroContext.<Deque<Context<?>>>getUndoParameter().value().forEach(context -> assertThat(context.isDone()).isTrue());
        verify(executor, times(command.fromNest().size())).execute(any(Runnable.class));
    }

    @Test
    <T> void shouldDoParallelCommand_ExtraCommands() {
        int parameter = 102;
        Input<Integer> inputParameter = Input.of(parameter);
        command = spy(new FakeParallelCommand(executor, studentCommand, actionExecutor));
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
        assertThat(overrideStudentContext).isEqualTo(wrapper.getNestedContexts().getFirst());
        verifyNestedCommandContextPreparation(command, doubleCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, booleanCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, intCommand, inputParameter);
        verify(studentCommand).acceptPreparedContext(command, inputParameter);
        verify(command).prepareContext(studentCommand, inputParameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10.0);
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));

        // doing command with macro-context
        command.doCommand(macroContext);

        // after do check
        verify(command).executeDo(macroContext);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(macroContext.isDone()).isTrue();
        assertThat(macroContext.<Deque<Context<T>>>getUndoParameter().value()).hasSameSizeAs(wrapper.getNestedContexts());
        wrapper.getNestedContexts().stream()
                .filter(context -> context != overrideStudentContext)
                .forEach(context -> {
                    assertThat(context.isDone()).isTrue();
                    verify(command).executeDoNested(eq(context), any(Context.StateChangedListener.class));
                    Context<T> nestedContext = (Context<T>) context;
                    verify(nestedContext.getCommand()).doCommand(nestedContext);
                });
        verify(command).executeDoNested(eq(overrideStudentContext), any(Context.StateChangedListener.class));
        verify(studentCommand, never()).doCommand(any(Context.class));
        macroContext.<Deque<Context<T>>>getUndoParameter().value().forEach(context -> assertThat(context.isDone()).isTrue());
        verify(executor, times(command.fromNest().size())).execute(any(Runnable.class));
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
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        doThrow(UnableExecuteCommandException.class).when(doubleCommand).doCommand(any(Context.class));

        // doing command with macro-context
        command.doCommand(macroContext);

        // after do check
        verify(command).executeDo(macroContext);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(macroContext.isFailed()).isTrue();
        assertThat(macroContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        assertThat(macroContext.<Deque<Context<T>>>getUndoParameter().isEmpty()).isTrue();
        // check nested command contexts behavior
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        wrapper.getNestedContexts().stream()
                .filter(context -> context.getCommand() != doubleCommand)
                .forEach(context -> {
                    assertThat(context.isDone()).isTrue();
                    verify(command).executeDoNested(eq(context), any(Context.StateChangedListener.class));
                    Context<T> nestedContext = (Context<T>) context;
                    verify(nestedContext.getCommand()).doCommand(nestedContext);
                    verify(command).executeUndoNested(context);
                });
        // check double command context behavior
        Context<T> doubleContext = (Context<T>) wrapper.getNestedContexts().getFirst();
        assertThat(doubleContext.isFailed()).isTrue();
        assertThat(doubleContext.getCommand()).isSameAs(doubleCommand);
        verify(doubleContext.getCommand()).doCommand(doubleContext);
        verify(command, never()).executeUndoNested(doubleContext);
        // check executor behavior
        int submitted = command.fromNest().size() * 2 - 1;
        verify(executor, times(submitted)).execute(any(Runnable.class));
    }

    @Test
    <T> void shouldRollbackAllNestedDoneContexts_BaseCommands() {
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
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).rollbackAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        command.doCommand(macroContext);
        Deque<Context<?>> nestedUndoneContexts = macroContext.<Deque<Context<?>>>getUndoParameter().value();
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);
        reset(executor);

        Deque<Context<?>> rollbackResults = command.rollbackNested(nestedUndoneContexts);

        assertThat(nestedUndoneContexts).hasSameSizeAs(rollbackResults);
        // check contexts states
        rollbackResults.forEach(context -> {
            assertThat(context.isUndone()).isTrue();
            verify(command).executeDoNested(eq(context), any(Context.StateChangedListener.class));
            Context<T> nestedContext = (Context<T>) context;
            verify(nestedContext.getCommand()).doCommand(nestedContext);
            verify(command).executeUndoNested(context);
            verify(context.getCommand()).undoCommand(context);
        });
        verify(executor, times(rollbackResults.size())).execute(any(Runnable.class));
    }

    @Test
    <T> void shouldUndoParallelCommand_BaseCommands() {
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
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).rollbackAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        command.doCommand(macroContext);
        assertThat(macroContext.isDone()).isTrue();
        Deque<Context<?>> nestedDoneContexts = macroContext.<Deque<Context<?>>>getUndoParameter().value();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.getState()).isEqualTo(DONE));
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);
        reset(executor);

        command.undoCommand(macroContext);

        assertThat(macroContext.getState()).isEqualTo(UNDONE);

        verify(command).executeUndo(macroContext);
        verify(command).rollbackNested(nestedDoneContexts);
        nestedDoneContexts.forEach(context -> {
            assertThat(context.isUndone()).isTrue();
            Context<T> nestedContext = (Context<T>) context;
            verify(command).executeDoNested(eq(context), any(Context.StateChangedListener.class));
            verify(nestedContext.getCommand()).doCommand(nestedContext);
            verify(command).executeUndoNested(context);
            verify(context.getCommand()).undoCommand(context);
        });
        verify(executor, times(nestedDoneContexts.size())).execute(any(Runnable.class));
    }

    @Test
    <T> void shouldUndoParallelCommand_ExtraCommands() {
        int parameter = 105;
        Input<Integer> inputParameter = Input.of(parameter);
        command = spy(new FakeParallelCommand(executor, studentCommand, actionExecutor));
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
        assertThat(overrideStudentContext).isEqualTo(wrapper.getNestedContexts().getFirst());
        verifyNestedCommandContextPreparation(command, doubleCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, booleanCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, intCommand, inputParameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10.0);
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).rollbackAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        command.doCommand(macroContext);
        assertThat(macroContext.isDone()).isTrue();
        Deque<Context<?>> nestedDoneContexts = macroContext.<Deque<Context<?>>>getUndoParameter().value();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.isDone()).isTrue());
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);
        reset(executor);

        command.undoCommand(macroContext);

        assertThat(macroContext.getState()).isEqualTo(UNDONE);
        nestedDoneContexts.stream()
                .filter(context -> context != overrideStudentContext)
                .forEach(context -> assertThat(context.getState()).isEqualTo(UNDONE));
        nestedDoneContexts.stream()
                .filter(context -> context != overrideStudentContext)
                .forEach(context -> {
                    assertThat(context.isUndone()).isTrue();
                    verify(command).executeDoNested(eq(context), any(Context.StateChangedListener.class));
                    Context<T> nestedContext = (Context<T>) context;
                    verify(nestedContext.getCommand()).doCommand(nestedContext);
                    verify(command).executeUndoNested(context);
                    verify(context.getCommand()).undoCommand(context);
                });
        assertThat(overrideStudentContext.getState()).isEqualTo(CANCEL);
        verify(command).executeUndo(macroContext);
        verify(command).rollbackNested(nestedDoneContexts);
        verify(executor, times(nestedDoneContexts.size())).execute(any(Runnable.class));
    }

    @Test
    <T> void shouldNotUndoParallelCommand_undoCommandThrowsException() {
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
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).rollbackAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        command.doCommand(macroContext);
        assertThat(macroContext.isDone()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> nestedDoneContexts = macroContext.<Deque<Context<?>>>getUndoParameter().value();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.isDone()).isTrue());
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);
        doThrow(UnableExecuteCommandException.class).when(doubleCommand).undoCommand(any(Context.class));
        reset(executor);

        command.undoCommand(macroContext);

        assertThat(macroContext.isFailed()).isTrue();
        verify(command).executeUndo(macroContext);
        verify(command).rollbackNested(nestedDoneContexts);
        nestedDoneContexts.stream().filter(context -> context.getCommand() != doubleCommand)
                .forEach(context -> {
                    assertThat(context.isDone()).isTrue();
                    verify(command).executeDoNested(eq(context), any(Context.StateChangedListener.class));
                    Context<T> nestedContext = (Context<T>) context;
                    verify(nestedContext.getCommand(), times(2)).doCommand(nestedContext);
                    verify(command).executeUndoNested(context);
                    verify(context.getCommand()).undoCommand(context);
                });
        Context<?> doubleContext = wrapper.getNestedContexts().getFirst();
        assertThat(doubleContext.isFailed()).isTrue();
        assertThat(macroContext.getException()).isEqualTo(doubleContext.getException());
        verify(executor, times(nestedDoneContexts.size())).execute(any(Runnable.class));
    }

    //    // inner classes
    static class FakeParallelCommand extends LegacyParallelMacroCommand<Double> {
        static CommandContext<Double> overrideStudentContext;
        private final Logger logger = LoggerFactory.getLogger(FakeParallelCommand.class);
        private final SchedulingTaskExecutor commandContextExecutor;

        public FakeParallelCommand(SchedulingTaskExecutor commandContextExecutor, StudentCommand<Double> student, ActionExecutor actionExecutor) {
            super(actionExecutor);
            this.commandContextExecutor = commandContextExecutor;
            overrideStudentContext = CommandContext.<Double>builder().command(student).state(INIT).build();
        }

        @Override
        public <T> Context<T> prepareContext(StudentCommand<T> command, Input<?> mainInput) {
            overrideStudentContext.setRedoParameter(Input.of(200));
            return (Context<T>) overrideStudentContext;
        }

        @Override
        public <N> Context<N> executeDoNested(Context<N> doContext, Context.StateChangedListener listener) {
            if (doContext.getCommand() instanceof StudentCommand) {
                doNestedStudentCommand(doContext, listener);
                return doContext;
            }
            return super.executeDoNested(doContext, listener);
        }

        @Override
        public Context<?> executeUndoNested(Context<?> undoContext) {
            if (undoContext.getCommand() instanceof StudentCommand) {
                undoContext.setState(CANCEL);
                return undoContext;
            }
            return super.executeUndoNested(undoContext);
        }

        void doNestedStudentCommand(Context<?> doContext, Context.StateChangedListener stateListener) {
            doContext.addStateListener(stateListener);
            doContext.setState(WORK);
            ((Context<Double>) doContext).setResult(100.0);
            doContext.removeStateListener(stateListener);
        }

        @Override
        public Logger getLog() {
            return logger;
        }

        @Override
        public String getId() {
            return "parallel-fake-command";
        }

        @Override
        public Double detachedResult(Double result) {
            return result;
        }

        @Override
        public BusinessMessagePayloadMapper getPayloadMapper() {
            return null;
        }

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
        assertThat(nestedContext.getCommand()).isSameAs(nestedCommand);
        assertThat(nestedContext.isDone()).isTrue();
        verify(command).executeDoNested(nestedContext, listener);
        verify(nestedContext.getCommand()).doCommand(nestedContext);
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