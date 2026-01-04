package oleg.sopilnyak.test.service.command.executable.core;

import static oleg.sopilnyak.test.service.command.executable.core.SequentialMacroCommandTest.FakeSequentialCommand.overrideCourseContext;
import static oleg.sopilnyak.test.service.command.executable.core.SequentialMacroCommandTest.FakeSequentialCommand.overrideStudentContext;
import static oleg.sopilnyak.test.service.command.type.core.Context.State.CANCEL;
import static oleg.sopilnyak.test.service.command.type.core.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.core.Context.State.FAIL;
import static oleg.sopilnyak.test.service.command.type.core.Context.State.INIT;
import static oleg.sopilnyak.test.service.command.type.core.Context.State.READY;
import static oleg.sopilnyak.test.service.command.type.core.Context.State.UNDONE;
import static oleg.sopilnyak.test.service.command.type.core.Context.State.WORK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.service.command.executable.core.executor.CommandActionExecutor;
import oleg.sopilnyak.test.service.command.executable.core.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.core.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.command.type.core.nested.NestedCommand;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class SequentialMacroCommandTest {
    @Spy
    @InjectMocks
    volatile FakeSequentialCommand command;
    @Mock
    volatile RootCommand<?> doubleCommand;
    @Mock
    volatile RootCommand<?> booleanCommand;
    @Mock
    volatile RootCommand<?> intCommand;
    @Mock
    volatile StudentCommand<?> studentCommand;
    @Mock
    volatile CourseCommand<?> courseCommand;
    @Mock
    CommandActionExecutor actionExecutor;

    @BeforeEach
    void setUp() {
        command.putToNest(doubleCommand);
        command.putToNest(booleanCommand);
        command.putToNest(intCommand);
        setupBaseCommandIds();
        ActionContext.setup("test-facade", "test-doingMainLoop");
    }

    @Test
    void checkSequentialCommandIntegrity() {
        reset(doubleCommand, booleanCommand, intCommand);
        assertThat(command).isNotNull();
        Deque<NestedCommand<?>> commands = new LinkedList<>(command.fromNest());
        assertThat(commands.pop()).isEqualTo(doubleCommand);
        assertThat(commands.pop()).isEqualTo(booleanCommand);
        assertThat(commands.pop()).isEqualTo(intCommand);
        assertThat(commands).isEmpty();
    }

    @Test
    void shouldDoAllNestedCommand() {
        int parameter = 100;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
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
        // check contexts states and result transfer as well
        Deque<Context<?>> nestedContexts = new LinkedList<>(wrapper.getNestedContexts());
        checkRegularNestedCommandExecution(doubleCommand, nestedContexts.pop(), listener);
        checkRegularNestedCommandExecution(booleanCommand, nestedContexts.pop(), listener);
        checkRegularNestedCommandExecution(intCommand, nestedContexts.pop(), listener, true);
    }

    @Test
    void shouldDoSequentialCommand_BaseCommands() {
        int parameter = 111;
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
        macroContext.<Deque<Context<?>>>getUndoParameter().value().forEach(context -> assertThat(context.isDone()).isTrue());
        // check contexts states and result transfer as well
        checkNestedCommandTransfers(wrapper);
        // check transfer absence for student-command
        verify(command, never()).transferPreviousExecuteDoResult(any(StudentCommand.class), any(), any(Context.class));
    }

    @Test
    void shouldDoSequentialCommand_ExtraCommands() {
        int parameter = 121;
        Input<Integer> inputParameter = Input.of(parameter);
        command = spy(new FakeSequentialCommand(studentCommand, courseCommand, actionExecutor));
        command.putToNest(studentCommand);
        command.putToNest(courseCommand);
        command.putToNest(doubleCommand);
        command.putToNest(booleanCommand);
        command.putToNest(intCommand);
        allowRealPrepareContextBase(inputParameter);
        allowRealPrepareContext(studentCommand, inputParameter);
        allowRealPrepareContext(courseCommand, inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        verifyNestedCommandContextPreparation(command, studentCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, courseCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, doubleCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, booleanCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, intCommand, inputParameter);
        checkRedefinedPrepareCommandContext(macroContext);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10.0);
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        reset(studentCommand, courseCommand);
        doReturn("studentCommand").when(studentCommand).getId();
        doReturn("courseCommand").when(courseCommand).getId();

        // doing command with macro-context
        command.doCommand(macroContext);

        // after do check
        verify(command).executeDo(macroContext);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(macroContext.isDone()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(macroContext.<Deque<Context<?>>>getUndoParameter().value()).hasSameSizeAs(wrapper.getNestedContexts());
        macroContext.<Deque<Context<?>>>getUndoParameter().value().forEach(context -> assertThat(context.isDone()).isTrue());
        assertThat(overrideStudentContext.getResult()).contains(100.0);
        assertThat(overrideCourseContext.getResult()).contains(200.0);
        // check contexts states and result transfer as well
        checkNestedCommandTransfers(wrapper);
        // check transfer of extra commands activity
        verify(command).transferResult(eq(courseCommand), eq(200.0), any(Context.class));
        verify(command).transferResult(eq(studentCommand), eq(100.0), any(Context.class));
        // check transfer presence for student-command
        verify(command).transferPreviousExecuteDoResult(eq(studentCommand), eq(100.0), any(Context.class));
    }

    @Test
    void shouldUndoSequentialCommand_BaseCommands() {
        int parameter = 131;
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
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.isDone()).isTrue());
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);

        command.undoCommand(macroContext);

        assertThat(macroContext.isUndone()).isTrue();
        nestedDoneContexts.forEach(context -> assertThat(context.getState()).isEqualTo(UNDONE));

        verify(command).executeUndo(macroContext);
        verify(command).rollbackNested(nestedDoneContexts);
        nestedDoneContexts.forEach(context -> {
            assertThat(context.isUndone()).isTrue();
            verify(command).executeUndoNested(context);
            var nestedCommand = context.getCommand();
            verify(nestedCommand).undoCommand(context);
        });
    }

    @Test
    void shouldUndoSequentialCommand_ExtraCommands() {
        int parameter = 132;
        Input<Integer> inputParameter = Input.of(parameter);
        command = spy(new FakeSequentialCommand(studentCommand, courseCommand, actionExecutor));
        command.putToNest(studentCommand);
        command.putToNest(courseCommand);
        command.putToNest(doubleCommand);
        command.putToNest(booleanCommand);
        command.putToNest(intCommand);
        allowRealPrepareContextBase(inputParameter);
        allowRealPrepareContext(studentCommand, inputParameter);
        allowRealPrepareContext(courseCommand, inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        verifyNestedCommandContextPreparation(command, studentCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, courseCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, doubleCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, booleanCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, intCommand, inputParameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10.0);
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).rollbackAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        reset(studentCommand, courseCommand);
        doReturn("studentCommand").when(studentCommand).getId();
        doReturn("courseCommand").when(courseCommand).getId();
        command.doCommand(macroContext);
        assertThat(macroContext.isDone()).isTrue();
        Deque<Context<?>> nestedDoneContexts = macroContext.<Deque<Context<?>>>getUndoParameter().value();
        // check transfer activity
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.isDone()).isTrue());
        verify(command).transferResult(eq(courseCommand), eq(200.0), any(Context.class));
        verify(command).transferResult(eq(studentCommand), eq(100.0), any(Context.class));
        verify(command).transferPreviousExecuteDoResult(eq(studentCommand), eq(100.0), any(Context.class));
        configureNestedUndoStatus(courseCommand);
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);

        command.undoCommand(macroContext);

        assertThat(macroContext.isUndone()).isTrue();
        nestedDoneContexts = macroContext.<Deque<Context<?>>>getUndoParameter().value();
        verify(command).executeUndo(macroContext);
        verify(command).rollbackNested(nestedDoneContexts);
        nestedDoneContexts.stream()
                .filter(context -> context != overrideStudentContext)
                .filter(context -> context != overrideCourseContext)
                .forEach(context -> {
                    assertThat(context.isUndone()).isTrue();
                    verify(command).executeUndoNested(context);
                    verify(context.getCommand()).undoCommand(context);
                });
        assertThat(overrideCourseContext.getState()).isEqualTo(UNDONE);
        assertThat(overrideStudentContext.getState()).isEqualTo(CANCEL);
    }

    @Test
    <N> void shouldDoSequentialCommand_TransferNestedCommandResult() {
        int parameter = 112;
        Input<Integer> inputParameter = Input.of(parameter);
        command = spy(new FakeSequentialCommand(studentCommand, courseCommand, actionExecutor));
        command.putToNest(studentCommand);
        command.putToNest(courseCommand);
        command.putToNest(doubleCommand);
        command.putToNest(booleanCommand);
        command.putToNest(intCommand);
        allowRealPrepareContextBase(inputParameter);
        allowRealPrepareContext(studentCommand, inputParameter);
        allowRealPrepareContext(courseCommand, inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        checkRedefinedPrepareCommandContext(macroContext);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10.0);
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        reset(studentCommand, courseCommand);
        doReturn("studentCommand").when(studentCommand).getId();
        doReturn("courseCommand").when(courseCommand).getId();

        // doing command with macro-context
        command.doCommand(macroContext);

        // after do check
        verify(command).executeDo(macroContext);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(macroContext.isDone()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        // check contexts states and result transfer as well
        checkNestedCommandTransfers(wrapper);
        AtomicBoolean firstTime = new AtomicBoolean(true);
        wrapper.getNestedContexts().forEach(context -> {
            Context<N> nestedContext = (Context<N>) context;
            RootCommand<N> current = nestedContext.getCommand();
            if (current != studentCommand && current != courseCommand) {
                verify(current).doCommand(nestedContext);
            } else {
                verify(current, never()).doCommand(any(Context.class));
            }
            if (firstTime.get()) {
                firstTime.compareAndSet(false, true);
            } else {
                Object executionResult = context.getResult().orElseThrow();
                verify(command).transferResult(eq(current), eq(executionResult), any(Context.class));
            }
            assertThat(context.isDone()).isTrue();
            assertThat(context.getResult()).isPresent();
        });
        checkNestedCommandResultTransfer();
        wrapper.getNestedContexts().stream()
                .filter(context -> context != overrideCourseContext)
                .filter(context -> context != overrideStudentContext)
                .forEach(context -> assertThat(context.getRedoParameter().value()).isSameAs(parameter));
        assertThat(macroContext.<Deque<Context<?>>>getUndoParameter().value()).hasSameSizeAs(wrapper.getNestedContexts());
        macroContext.<Deque<Context<?>>>getUndoParameter().value().forEach(context -> assertThat(context.isDone()).isTrue());
    }

    @Test
    <T> void shouldNotDoSomeNestedRedo_NestedRedoThrown() {
        int parameter = 101;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContextBase(inputParameter);

        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.isReady()).isTrue());
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        doThrow(UnableExecuteCommandException.class).when(booleanCommand).doCommand(any(Context.class));
        AtomicInteger counter = new AtomicInteger(0);
        Context.StateChangedListener listener = spy(new ContextStateChangedListener(counter));
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        reset(intCommand);

        // executing nested commands at once
        command.executeNested(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isEqualTo(1);
        //
        // check doubleCommand behavior
        checkRegularNestedCommandExecution(doubleCommand, wrapper.getNestedContexts().pop(), listener);
        //
        // check booleanCommand behavior
        Context<T> nestedContext = (Context<T>) wrapper.getNestedContexts().pop();
        assertThat(nestedContext.isFailed()).isTrue();
        assertThat(nestedContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        verify(command).executeDoNested(nestedContext, listener);
        Context rawContext = nestedContext;
        verify(booleanCommand).doCommand(rawContext);
        verify(command, never()).transferResult(eq(booleanCommand), any(), any(Context.class));
        verify(listener).stateChanged(nestedContext, READY, FAIL);
        //
        // check intCommand behavior
        nestedContext = (Context<T>) wrapper.getNestedContexts().pop();
        assertThat(nestedContext.getState()).isEqualTo(CANCEL);
        verify(command, never()).executeDoNested(eq(nestedContext), any(Context.StateChangedListener.class));
        verify(intCommand, never()).doCommand(any(Context.class));
        verify(command, never()).transferResult(eq(intCommand), any(), any(Context.class));
        verify(listener).stateChanged(nestedContext, READY, CANCEL);
    }

    @Test
    <N> void shouldNotDoAllNestedRedo_NestedRedoThrown() {
        reset(doubleCommand, booleanCommand, intCommand);
        int parameter = 102;
        Input<Integer> inputParameter = Input.of(parameter);
        AtomicInteger counter = new AtomicInteger(0);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.isReady()).isTrue());
        doThrow(UnableExecuteCommandException.class).when(doubleCommand).doCommand(any(Context.class));
        Context.StateChangedListener listener = spy(new ContextStateChangedListener(counter));
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));

        // executing nested commands at once
        command.executeNested(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isZero();
        //
        // check doubleCommand behavior
        Context<N> nestedContext = (Context<N>) wrapper.getNestedContexts().pop();
        assertThat(nestedContext.isFailed()).isTrue();
        assertThat(nestedContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        verify(command).executeDoNested(nestedContext, listener);
        verify(doubleCommand).doCommand(any(Context.class));
        verify(listener).stateChanged(nestedContext, READY, FAIL);
        //
        // check booleanCommand behavior
        nestedContext = (Context<N>) wrapper.getNestedContexts().pop();
        verify(command, never()).executeDoNested(eq(nestedContext), any(Context.StateChangedListener.class));
        verify(booleanCommand, never()).doCommand(any(Context.class));
        assertThat(nestedContext.getState()).isEqualTo(CANCEL);
        verify(listener).stateChanged(nestedContext, READY, CANCEL);
        //
        // check intCommand behavior
        nestedContext = (Context<N>) wrapper.getNestedContexts().pop();
        verify(command, never()).executeDoNested(eq(nestedContext), any(Context.StateChangedListener.class));
        verify(intCommand, never()).doCommand(any(Context.class));
        assertThat(nestedContext.getState()).isEqualTo(CANCEL);
        verify(listener).stateChanged(nestedContext, READY, CANCEL);
    }

    @Test
    void shouldRollbackAllNestedDoneContexts_BaseCommands() {
        int parameter = 103;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.getState()).isEqualTo(READY));
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

        Deque<Context<?>> rollbackResults = command.rollbackNested(nestedUndoneContexts);

        assertThat(nestedUndoneContexts).hasSameSizeAs(rollbackResults);
        int size = nestedUndoneContexts.size();
        List<Context<?>> params = nestedUndoneContexts.stream().toList();
        List<Context<?>> undone = rollbackResults.stream().toList();
        // check revers
        IntStream.range(0, size).forEach(i -> assertThat(undone.get(i)).isEqualTo(params.get(i)));
        // check contexts states
        rollbackResults.forEach(context -> {
            assertThat(context.isUndone()).isTrue();
            verify(command).executeUndoNested(context);
            verify(context.getCommand()).undoCommand(context);
        });
    }

    @Test
    void shouldNotRollbackSomeNestedDoneContexts_NestedUndoThrown() {
        int parameter = 104;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.getState()).isEqualTo(READY));
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10.0);
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).rollbackAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        command.doCommand(macroContext);
        Deque<Context<?>> nestedUndoneContexts = macroContext.<Deque<Context<?>>>getUndoParameter().value();
        doThrow(UnableExecuteCommandException.class).when(doubleCommand).undoCommand(any(Context.class));
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);

        Deque<Context<?>> rollbackResults = command.rollbackNested(nestedUndoneContexts);

        assertThat(nestedUndoneContexts).hasSameSizeAs(rollbackResults);
        int size = nestedUndoneContexts.size();
        List<Context<?>> params = nestedUndoneContexts.stream().toList();
        List<Context<?>> undone = rollbackResults.stream().toList();
        // check revers
        IntStream.range(0, size).forEach(i -> assertThat(undone.get(i)).isEqualTo(params.get(i)));
        // check contexts order and states
        Context<?> nestedContext;
        //
        // check behavior of doubleCommand
        nestedContext = rollbackResults.pop();
        assertThat(nestedContext.isFailed()).isTrue();
        assertThat(nestedContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        verify(command).executeUndoNested(nestedContext);
        verify(doubleCommand).undoCommand(nestedContext);
        //
        // check behavior of booleanCommand
        nestedContext = rollbackResults.pop();
        assertThat(nestedContext.isUndone()).isTrue();
        verify(command).executeUndoNested(nestedContext);
        verify(booleanCommand).undoCommand(nestedContext);
        //
        // check behavior of intCommand
        nestedContext = rollbackResults.pop();
        assertThat(nestedContext.isUndone()).isTrue();
        verify(command).executeUndoNested(nestedContext);
        verify(intCommand).undoCommand(nestedContext);
    }

    @Test
    void shouldNotUndoSequentialCommand_NestedUndoThrown() {
        int parameter = 104;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.getState()).isEqualTo(READY));
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10.0);
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).rollbackAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));
        command.doCommand(macroContext);
        Deque<Context<?>> nestedUndoneContexts = macroContext.<Deque<Context<?>>>getUndoParameter().value();
        doThrow(new UnableExecuteCommandException("double-command is forbidden")).when(doubleCommand).undoCommand(any(Context.class));
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);

        command.undoCommand(macroContext);

        wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> rollbackResults = wrapper.getNestedContexts();
        assertThat(nestedUndoneContexts).hasSameSizeAs(rollbackResults);
        int size = nestedUndoneContexts.size();
        List<Context<?>> params = nestedUndoneContexts.stream().toList();
        List<Context<?>> undone = rollbackResults.stream().toList();
        // check revers
        IntStream.range(0, size).forEach(i -> assertThat(undone.get(i)).isEqualTo(params.get(i)));
        // check contexts order and states
        Context<?> nestedContext;
        //
        // check behavior of doubleCommand
        nestedContext = rollbackResults.pop();
        assertThat(nestedContext.isFailed()).isTrue();
        assertThat(nestedContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        verify(command).executeUndoNested(nestedContext);
        verify(doubleCommand).undoCommand(nestedContext);
        //
        // check behavior of booleanCommand
        nestedContext = rollbackResults.pop();
        assertThat(nestedContext.isDone()).isTrue();
        verify(command).executeUndoNested(nestedContext);
        verify(command).executeDoNested(nestedContext);
        verify(booleanCommand).undoCommand(nestedContext);
        verify(booleanCommand, times(2)).doCommand(any(Context.class));
        //
        // check behavior of intCommand
        nestedContext = rollbackResults.pop();
        assertThat(nestedContext.isDone()).isTrue();
        verify(command).executeUndoNested(nestedContext);
        verify(command).executeDoNested(nestedContext);
        verify(intCommand).undoCommand(nestedContext);
        verify(intCommand, times(2)).doCommand(any(Context.class));
    }

    static class FakeSequentialCommand extends SequentialMacroCommand<Double> {
        static CommandContext<Double> overrideStudentContext;
        static CommandContext<Double> overrideCourseContext;
        private final Logger logger = LoggerFactory.getLogger(FakeSequentialCommand.class);

        public FakeSequentialCommand(StudentCommand<?> student, CourseCommand<?> course, CommandActionExecutor actionExecutor) {
            super(actionExecutor);
            overrideStudentContext = CommandContext.<Double>builder().command((RootCommand<Double>) student).state(INIT).build();
            overrideCourseContext = CommandContext.<Double>builder().command((RootCommand<Double>) course).state(INIT).build();
        }

        @Override
        public Logger getLog() {
            return logger;
        }

        @Override
        public String getId() {
            return "sequential-fake-command";
        }

        @Override
        public <N> Context<N> prepareContext(StudentCommand<N> command, Input<?> mainInput) {
            return prepareStudentContext();
        }

        @Override
        public <N> Context<N> prepareContext(CourseCommand<N> command, Input<?> mainInput) {
            return prepareCourseContext();
        }

        @Override
        public <N> Context<N> executeDoNested(Context<N> doContext, Context.StateChangedListener listener) {
            RootCommand<?> nestedCommand = doContext.getCommand();
            if (nestedCommand instanceof StudentCommand) {
                doContext.addStateListener(listener);
                doContext.setState(WORK);
                ((Context<Double>) doContext).setResult(100.0);
                doContext.removeStateListener(listener);
                return doContext;
            }
            if (nestedCommand instanceof CourseCommand) {
                doContext.addStateListener(listener);
                doContext.setState(WORK);
                ((Context<Double>) doContext).setResult(200.0);
                doContext.removeStateListener(listener);
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

        @Override
        public void transferResult(RootCommand<?> executed, Object result, Context<?> toExecute) {
            if (executed instanceof StudentCommand studentCommand) {
                transferPreviousExecuteDoResult(studentCommand, result, toExecute);
            }
        }

        public <S, T> void transferPreviousExecuteDoResult(
                final StudentCommand<?> command, final S result, final Context<T> target
        ) {
            getLog().info("Transfer student-command '{}' result:{} to {}", command, result, target);
            if (target instanceof CommandContext<T> commandContext) {
                commandContext.setRedoParameter(Input.of((Double) result + 1000));
            } else {
                throw new InvalidParameterTypeException(CommandContext.class.getSimpleName(), target.getClass().getSimpleName());
            }
            getLog().info("Transferred student-command result context:{}", target);
        }
    }

    // private methods
    private void setupBaseCommandIds() {
        doReturn("doubleCommand").when(doubleCommand).getId();
        doReturn("booleanCommand").when(booleanCommand).getId();
        doReturn("intCommand").when(intCommand).getId();
    }

    private static void checkNestedCommandResultTransfer() {
        Double studentResult = overrideStudentContext.getResult().orElseThrow() + 1000;
        Double courseParameter = ((Number) overrideCourseContext.getRedoParameter().value()).doubleValue();
        assertThat(courseParameter).isEqualTo(studentResult);
    }

    private static void checkRedefinedPrepareCommandContext(Context<Double> macroContext) {
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        Deque<Context<?>> contexts = new LinkedList<>(wrapper.getNestedContexts());
        assertThat(contexts.pop()).isEqualTo(overrideStudentContext);
        assertThat(contexts.pop()).isEqualTo(overrideCourseContext);
    }

    private <T> void checkRegularNestedCommandExecution(
            RootCommand<?> nestedCommand, Context<T> nestedContext, Context.StateChangedListener listener
    ) {
        checkRegularNestedCommandExecution(nestedCommand, nestedContext, listener, false);
    }

    private <T> void checkRegularNestedCommandExecution(
            RootCommand nestedCommand, Context<T> nestedContext, Context.StateChangedListener listener, boolean lastNestedCommand
    ) {
        assertThat(nestedContext.isDone()).isTrue();
        verify(command).executeDoNested(nestedContext, listener);
        verify(nestedCommand).doCommand(nestedContext);
        assertNestedTransfer(nestedContext, lastNestedCommand);
        verify(listener).stateChanged(nestedContext, READY, WORK);
        verify(listener).stateChanged(nestedContext, WORK, DONE);
    }

    private void checkNestedCommandTransfers(MacroCommandParameter wrapper) {
        Deque<Context<?>> nestedContexts = new LinkedList<>(wrapper.getNestedContexts());
        int nestedCount = nestedContexts.size();
        wrapper.getNestedContexts().stream().limit(nestedCount - 1)
                .forEach(_ -> assertNestedTransfer(nestedContexts.pop(), false));
        assertNestedTransfer(nestedContexts.pop(), true);
    }

    private <T> void assertNestedTransfer(Context<T> nestedContext, boolean lastNestedCommand) {
        RootCommand<T> nestedCommand = nestedContext.getCommand();
        if (lastNestedCommand) {
            verify(command, never()).transferResult(eq(nestedCommand), any(), any(Context.class));
        } else {
            T executionResult = nestedContext.getResult().orElseThrow();
            verify(command).transferResult(eq(nestedCommand), eq(executionResult), any(Context.class));
        }
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

    private <T> void configureNestedRedoResult(RootCommand<?> nestedCommand, T result) {
        doAnswer(invocationOnMock -> {
            Context<T> context = invocationOnMock.getArgument(0, Context.class);
            context.setState(WORK);
            context.setResult(result);
            return null;
        }).when(nestedCommand).doCommand(any(Context.class));
    }

    private void configureNestedUndoStatus(RootCommand<?> nestedCommand) {
        doAnswer(invocationOnMock -> {
            Context<?> context = invocationOnMock.getArgument(0, Context.class);
            context.setState(WORK);
            context.setState(UNDONE);
            return null;
        }).when(nestedCommand).undoCommand(any(Context.class));
    }

    private static void verifyNestedCommandContextPreparation(FakeSequentialCommand command,
                                                              RootCommand<?> nestedCommand,
                                                              Input<?> parameter) {
        verify(nestedCommand).acceptPreparedContext(command, parameter);
        verify(command).prepareContext(nestedCommand, parameter);
    }

    private static void verifyNestedCommandContextPreparation(FakeSequentialCommand command,
                                                              StudentCommand<?> nestedCommand,
                                                              Input<?> parameter) {
        verify(nestedCommand).acceptPreparedContext(command, parameter);
        verify(command).prepareContext(nestedCommand, parameter);
    }

    private static void verifyNestedCommandContextPreparation(FakeSequentialCommand command,
                                                              CourseCommand<?> nestedCommand,
                                                              Input<?> parameter) {
        verify(nestedCommand).acceptPreparedContext(command, parameter);
        verify(command).prepareContext(nestedCommand, parameter);
    }

    private static <T> Context<T> prepareStudentContext() {
        overrideStudentContext.setRedoParameter(Input.of(200));
        return (Context<T>) overrideStudentContext;
    }

    private static <T> Context<T> prepareCourseContext() {
        overrideCourseContext.setRedoParameter(Input.of(300));
        return (Context<T>) overrideCourseContext;
    }

    // inner classes
    static class ContextStateChangedListener implements Context.StateChangedListener {
        private final AtomicInteger counter;

        ContextStateChangedListener(AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        public void stateChanged(Context<?> context, Context.State previous, Context.State current) {
            if (current == DONE) counter.incrementAndGet();
        }
    }
}
