package oleg.sopilnyak.test.service.command.executable.sys;

import static oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommandTest.FakeSequentialCommand.overrideCourseContext;
import static oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommandTest.FakeSequentialCommand.overrideStudentContext;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.CANCEL;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.FAIL;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.INIT;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.READY;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.WORK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import oleg.sopilnyak.test.school.common.business.facade.ActionContext;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.command.type.nested.CommandInSequence;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.TransferTransitionalResultVisitor;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.BaseCommandMessage;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
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
import org.springframework.lang.NonNull;

@ExtendWith(MockitoExtension.class)
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
    ActionExecutor actionExecutor;

    @BeforeEach
    void setUp() {
        command.putToNest(doubleCommand);
        command.putToNest(booleanCommand);
        command.putToNest(intCommand);
        wrapBaseCommands();
        ActionContext.setup("test-facade", "test-processing");
    }

    @Test
    void checkSequentialCommandIntegrity() {
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
        // check contexts states
        checkRegularNestedCommandExecution(doubleCommand, wrapper.getNestedContexts().pop(), listener);
        checkRegularNestedCommandExecution(booleanCommand, wrapper.getNestedContexts().pop(), listener);
        checkRegularNestedCommandExecution(intCommand, wrapper.getNestedContexts().pop(), listener, true);
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
//        RootCommand<?> pureDoubleCommand = doubleCommand instanceof SequentialMacroCommand.Chained<?> nested ? nested.unWrap() : doubleCommand;
//        RootCommand<?> pureBooleanCommand = booleanCommand instanceof SequentialMacroCommand.Chained<?> nested ? nested.unWrap() : booleanCommand;
//        verify(command).transferPreviousExecuteDoResult(eq(pureDoubleCommand), any(), any(Context.class));
//        verify(command).transferPreviousExecuteDoResult(eq(pureBooleanCommand), any(), any(Context.class));
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
        wrapAllCommands();
        allowRealPrepareContextBase(inputParameter);
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
//            nestedCommand = nestedCommand instanceof SequentialMacroCommand.Chained<?> wrapped ? wrapped.unWrap() : nestedCommand;
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
        wrapAllCommands();
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
        command.doCommand(macroContext);
        assertThat(macroContext.isDone()).isTrue();
        Deque<Context<?>> nestedDoneContexts = macroContext.<Deque<Context<?>>>getUndoParameter().value();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.isDone()).isTrue());
        configureNestedUndoStatus(courseCommand);
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);

        command.undoCommand(macroContext);

        assertThat(macroContext.isUndone()).isTrue();
        verify(command).executeUndo(macroContext);
        verify(command).rollbackNested(nestedDoneContexts);
        nestedDoneContexts.stream()
                .filter(context -> context != overrideStudentContext)
                .filter(context -> context != overrideCourseContext)
                .forEach(context -> {
                    assertThat(context.isUndone()).isTrue();
                    verify(command).executeUndoNested(context);
                    var nestedCommand = context.getCommand();
//                    nestedCommand = nestedCommand instanceof SequentialMacroCommand.Chained<?> wrapped ? wrapped.unWrap() : nestedCommand;
                    verify(nestedCommand).undoCommand(context);
                });
        assertThat(overrideCourseContext.isUndone()).isTrue();
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
        wrapAllCommands();
        Context<Double> macroContext = command.createContext(inputParameter);
        checkRedefinedPrepareCommandContext(macroContext);
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
        AtomicBoolean firstTime = new AtomicBoolean(true);
//        RootCommand<?> unWrappedInt = ((SequentialMacroCommand.Chained<?>) intCommand).unWrap();
//        RootCommand<?> unWrappedStudent = ((SequentialMacroCommand.Chained<?>) studentCommand).unWrap();
//        RootCommand<?> unWrappedCourse = ((SequentialMacroCommand.Chained<?>) courseCommand).unWrap();
        wrapper.getNestedContexts().forEach(context -> {
            Context<N> nestedContext = (Context<N>) context;
            RootCommand<N> current = nestedContext.getCommand();
//            RootCommand<N> unWrappedCurrent = (RootCommand<N>) ((SequentialMacroCommand.Chained<?>) current).unWrap();
//            verify(command).executeDoNested(eq(nestedContext), any(Context.StateChangedListener.class));
//            if (unWrappedCurrent != unWrappedStudent && unWrappedCurrent != unWrappedCourse) {
//                verify(current).doCommand(nestedContext);
//            } else {
//                verify(current, never()).doCommand(nestedContext);
//            }
//            if (unWrappedCurrent != unWrappedInt) {
////                verify((SequentialMacroCommand.Chained<?>) current).transferResultTo(eq(command), any(), any(Context.class));
//            }
            if (firstTime.get()) {
                firstTime.compareAndSet(false, true);
            } else {
//                verify(command).transferPreviousExecuteDoResult(eq(current), any(Optional.class), eq(context));
            }
            assertThat(context.isDone()).isTrue();
            assertThat(context.getResult()).isPresent();
        });
//        verify(command).transferPreviousExecuteDoResult(any(StudentCommand.class), any(), any());
//        verify(command).transferPreviousExecuteDoResult(any(CourseCommand.class), any(), any());
//        verify(command, times(2)).transferPreviousExecuteDoResult(any(RootCommand.class), any(), any());
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
//        RootCommand<T> pureBooleanCommand = booleanCommand instanceof SequentialMacroCommand.Chained<?> nestedWrapper ?
//                (RootCommand<T>) nestedWrapper.unWrap() : (RootCommand<T>) booleanCommand;
//        doThrow(UnableExecuteCommandException.class).when(pureBooleanCommand).doCommand(any(Context.class));
        AtomicInteger counter = new AtomicInteger(0);
        Context.StateChangedListener listener = spy(new ContextStateChangedListener(counter));
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));

        // executing nested commands at once
        command.executeNested(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isEqualTo(1);
        //
        // check doubleCommand behaviour
        checkRegularNestedCommandExecution(doubleCommand, wrapper.getNestedContexts().pop(), listener);
        //
        // check booleanCommand behaviour
        Context<T> nestedContext = (Context<T>) wrapper.getNestedContexts().pop();
        assertThat(nestedContext.isFailed()).isTrue();
        assertThat(nestedContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        verify(command).executeDoNested(nestedContext, listener);
//        verify(pureBooleanCommand).doCommand(nestedContext);
//        verify(command, never()).transferPreviousExecuteDoResult(eq(pureBooleanCommand), any(), any(Context.class));
        verify(listener).stateChanged(nestedContext, READY, FAIL);
        //
        // check intCommand behaviour
        nestedContext = (Context<T>) wrapper.getNestedContexts().pop();
        assertThat(nestedContext.getState()).isEqualTo(CANCEL);
        verify(command, never()).executeDoNested(eq(nestedContext), any(Context.StateChangedListener.class));
        verify(intCommand, never()).doCommand(any(Context.class));
//        verify(command, never()).transferPreviousExecuteDoResult(eq(intCommand), any(), any(Context.class));
        verify(listener).stateChanged(nestedContext, READY, CANCEL);
    }

    @Test
    <N> void shouldNotDoAllNestedRedo_NestedRedoThrown() {
        int parameter = 102;
        Input<Integer> inputParameter = Input.of(parameter);
        AtomicInteger counter = new AtomicInteger(0);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.isReady()).isTrue());
//        RootCommand<N> pureDoubleCommand = doubleCommand instanceof SequentialMacroCommand.Chained<?> nestedWrapper ?
//                (RootCommand<N>) nestedWrapper.unWrap() : (RootCommand<N>) doubleCommand;
//        doThrow(UnableExecuteCommandException.class).when(pureDoubleCommand).doCommand(any(Context.class));
        Context.StateChangedListener listener = spy(new ContextStateChangedListener(counter));
        doCallRealMethod().when(actionExecutor).commitAction(eq(ActionContext.current()), any(Context.class));
        doCallRealMethod().when(actionExecutor).processActionCommand(any(BaseCommandMessage.class));

        // executing nested commands at once
        command.executeNested(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isZero();
        //
        // check doubleCommand behaviour
        Context<N> nestedContext = (Context<N>) wrapper.getNestedContexts().pop();
        assertThat(nestedContext.isFailed()).isTrue();
        assertThat(nestedContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        verify(command).executeDoNested(nestedContext, listener);
        verify(doubleCommand).doCommand(any(Context.class));
        verify(listener).stateChanged(nestedContext, READY, FAIL);
        //
        // check booleanCommand behaviour
        nestedContext = (Context<N>) wrapper.getNestedContexts().pop();
        verify(command, never()).executeDoNested(eq(nestedContext), any(Context.StateChangedListener.class));
        verify(booleanCommand, never()).doCommand(any(Context.class));
        assertThat(nestedContext.getState()).isEqualTo(CANCEL);
        verify(listener).stateChanged(nestedContext, READY, CANCEL);
        //
        // check intCommand behaviour
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
//        RootCommand<?> pureDoubleCommand = doubleCommand instanceof SequentialMacroCommand.Chained<?> nestedWrapper ?
//                nestedWrapper.unWrap() : doubleCommand;
//        RootCommand<?> pureBoolCommand = booleanCommand instanceof SequentialMacroCommand.Chained<?> nestedWrapper ?
//                nestedWrapper.unWrap() : booleanCommand;
//        RootCommand<?> pureIntCommand = intCommand instanceof SequentialMacroCommand.Chained<?> nestedWrapper ?
//                nestedWrapper.unWrap() : intCommand;
//        doThrow(UnableExecuteCommandException.class).when(pureDoubleCommand).undoCommand(any(Context.class));
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
//        verify(pureDoubleCommand).undoCommand(nestedContext);
        //
        // check behavior of booleanCommand
        nestedContext = rollbackResults.pop();
        assertThat(nestedContext.isUndone()).isTrue();
        verify(command).executeUndoNested(nestedContext);
        verify(booleanCommand).undoCommand(nestedContext);
//        verify(pureBoolCommand).undoCommand(nestedContext);
        //
        // check behavior of intCommand
        nestedContext = rollbackResults.pop();
        assertThat(nestedContext.isUndone()).isTrue();
        verify(command).executeUndoNested(nestedContext);
        verify(intCommand).undoCommand(nestedContext);
//        verify(pureIntCommand).undoCommand(nestedContext);
    }

    static class FakeSequentialCommand extends SequentialMacroCommand<Double> {
        static CommandContext<Double> overrideStudentContext;
        static CommandContext<Double> overrideCourseContext;
        private final Logger logger = LoggerFactory.getLogger(FakeSequentialCommand.class);

        public FakeSequentialCommand(StudentCommand<?> student, CourseCommand<?> course, ActionExecutor actionExecutor) {
            super(actionExecutor);
//            overrideStudentContext = CommandContext.<Double>builder().command((RootCommand<Double>) wrap(student)).state(INIT).build();
//            overrideCourseContext = CommandContext.<Double>builder().command((RootCommand<Double>) wrap(course)).state(INIT).build();
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
        public Double detachedResult(Double result) {
            return result;
        }

        @Override
        public BusinessMessagePayloadMapper getPayloadMapper() {
            return null;
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
//            if (nestedCommand instanceof SequentialMacroCommand.Chained<?> inSequenceCommand) {
//                nestedCommand = inSequenceCommand.unWrap();
//            }
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
            RootCommand<?> nestedCommand = undoContext.getCommand();
//            if (nestedCommand instanceof SequentialMacroCommand.Chained<?> inSequenceCommand) {
//                nestedCommand = inSequenceCommand.unWrap();
//            }
            if (nestedCommand instanceof StudentCommand) {
                undoContext.setState(CANCEL);
                return undoContext;
            }
            return super.executeUndoNested(undoContext);
        }

//        @Override
        public <S, T> void transferPreviousExecuteDoResult(final StudentCommand<?> command,
                                                           final S result, final Context<T> target) {
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

    private <T> void checkRegularNestedCommandExecution(RootCommand<?> nestedCommand, Context<T> nestedContext, Context.StateChangedListener listener) {
        checkRegularNestedCommandExecution(nestedCommand, nestedContext, listener, false);
    }

    private <T> void checkRegularNestedCommandExecution(RootCommand nestedCommand,
                                                        @NonNull Context<T> nestedContext,
                                                        Context.StateChangedListener listener,
                                                        boolean lastNestedCommand) {
        assertThat(nestedContext.isDone()).isTrue();
        T doResult = nestedContext.getResult().orElseThrow();
//        if (nestedCommand instanceof SequentialMacroCommand.Chained<?> inSequenceCommand) {
//            nestedCommand = inSequenceCommand.unWrap();
//        }
        verify(command).executeDoNested(nestedContext, listener);
        verify(nestedCommand).doCommand(nestedContext);
        if (lastNestedCommand) {
//            verify(command, never()).transferPreviousExecuteDoResult(eq(nestedCommand), any(), any(Context.class));
        } else {
//            verify(command).transferPreviousExecuteDoResult(eq(nestedCommand), eq(doResult), any(Context.class));
        }
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

    private <T> void configureNestedRedoResult(RootCommand<?> nestedCommand, T result) {
//        if (nestedCommand instanceof SequentialMacroCommand.Chained<?> inSequenceCommand) {
//            nestedCommand = inSequenceCommand.unWrap();
//        }
        doAnswer(invocationOnMock -> {
            Context<T> context = invocationOnMock.getArgument(0, Context.class);
            context.setState(WORK);
            context.setResult(result);
            return null;
        }).when(nestedCommand).doCommand(any(Context.class));
    }

    private void configureNestedUndoStatus(RootCommand<?> nestedCommand) {
//        if (nestedCommand instanceof SequentialMacroCommand.Chained<?> inSequenceCommand) {
//            nestedCommand = inSequenceCommand.unWrap();
//        }
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

    private void wrapBaseCommands() {
        Deque<NestedCommand<?>> commands = new LinkedList<>(command.fromNest());
        doubleCommand = (RootCommand<?>) commands.pop();
        booleanCommand = (RootCommand<?>) commands.pop();
        intCommand = (RootCommand<?>) commands.pop();
    }

    private void wrapAllCommands() {
        Deque<NestedCommand<?>> commands = new LinkedList<>(command.fromNest());
        studentCommand = (StudentCommand<?>) commands.pop();
        courseCommand = (CourseCommand<?>) commands.pop();
        doubleCommand = (RootCommand<?>) commands.pop();
        booleanCommand = (RootCommand<?>) commands.pop();
        intCommand = (RootCommand<?>) commands.pop();
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
