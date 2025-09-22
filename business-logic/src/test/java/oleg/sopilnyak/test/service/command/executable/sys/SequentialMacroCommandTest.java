package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.service.command.executable.ActionExecutor;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.education.CourseCommand;
import oleg.sopilnyak.test.service.command.type.education.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.TransferResultVisitor;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
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

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommandTest.FakeSequentialCommand.overrideCourseContext;
import static oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommandTest.FakeSequentialCommand.overrideStudentContext;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        allowRealNestedCommandExecutionBase();

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
        allowRealNestedCommandExecutionBase();

        // doing command with macro-context
        command.doCommand(macroContext);

        // after do check
        verify(command).executeDo(macroContext);
        verify(command).executeNested(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(macroContext.isDone()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(macroContext.<Deque<Context<?>>>getUndoParameter().value()).hasSameSizeAs(wrapper.getNestedContexts());
        macroContext.<Deque<Context<?>>>getUndoParameter().value().forEach(context -> assertThat(context.isDone()).isTrue());
        RootCommand<?> pureDoubleCommand = doubleCommand instanceof SequentialMacroCommand.Chained<?> nested ? nested.unWrap() : doubleCommand;
        RootCommand<?> pureBooleanCommand = booleanCommand instanceof SequentialMacroCommand.Chained<?> nested ? nested.unWrap() : booleanCommand;
        verify(command).transferPreviousExecuteDoResult(eq(pureDoubleCommand), any(), any(Context.class));
        verify(command).transferPreviousExecuteDoResult(eq(pureBooleanCommand), any(), any(Context.class));
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
        allowRealPrepareContextExtra(inputParameter);
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
        allowRealNestedCommandExecutionBase();
        allowRealNestedCommandExecutionExtra();

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
        allowRealNestedCommandExecutionBase();
        command.doCommand(macroContext);
        assertThat(macroContext.isDone()).isTrue();
        Deque<Context<?>> nestedDoneContexts = macroContext.<Deque<Context<?>>>getUndoParameter().value();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.isDone()).isTrue());
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);
        allowRealNestedCommandRollbackBase();

        command.undoCommand(macroContext);

        assertThat(macroContext.getState()).isEqualTo(UNDONE);
        nestedDoneContexts.forEach(context -> assertThat(context.getState()).isEqualTo(UNDONE));

        verify(command).executeUndo(macroContext);
        verify(command).rollbackNestedDone(Input.of(nestedDoneContexts));
        nestedDoneContexts.forEach(context -> {
            var nestedCommand = context.getCommand();
            nestedCommand = nestedCommand instanceof SequentialMacroCommand.Chained<?> wrapped ? wrapped.unWrap() : nestedCommand;
            verify(nestedCommand).undoAsNestedCommand(command, context);
            verify(command).undoNestedCommand(nestedCommand, context);
            verify(nestedCommand).undoCommand(context);
        });
    }

    @Test
    void shouldUndoSequentialCommand_ExtraCommands() {
        int parameter = 132;
        Input<Integer> inputParameter = Input.of(parameter);
        command = spy(new FakeSequentialCommand(studentCommand, courseCommand, actionExecutor ));
        command.putToNest(studentCommand);
        command.putToNest(courseCommand);
        command.putToNest(doubleCommand);
        command.putToNest(booleanCommand);
        command.putToNest(intCommand);
        wrapAllCommands();
        allowRealPrepareContextBase(inputParameter);
        allowRealPrepareContextExtra(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        verifyNestedCommandContextPreparation(command, studentCommand, inputParameter);
        verifyNestedCommandContextPreparation(command, courseCommand, inputParameter);
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
        configureNestedUndoStatus(courseCommand);
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);
        allowRealNestedCommandRollbackBase();
        allowRealNestedCommandRollbackExtra();

        command.undoCommand(macroContext);

        assertThat(macroContext.getState()).isEqualTo(UNDONE);
        nestedDoneContexts.stream()
                .filter(context -> context != overrideStudentContext)
                .forEach(context -> assertThat(context.getState()).isEqualTo(UNDONE));
        verify(command).executeUndo(macroContext);
        verify(command).rollbackNestedDone(Input.of(nestedDoneContexts));
        nestedDoneContexts.stream()
                .filter(context -> context != overrideStudentContext)
                .filter(context -> context != overrideCourseContext)
                .forEach(context -> {
                    var nestedCommand = context.getCommand();
                    nestedCommand = nestedCommand instanceof SequentialMacroCommand.Chained<?> wrapped ? wrapped.unWrap() : nestedCommand;
                    verify(nestedCommand).undoAsNestedCommand(command, context);
                    verify(command).undoNestedCommand(nestedCommand, context);
                    verify(nestedCommand).undoCommand(context);
                });
        assertThat(overrideCourseContext.getState()).isEqualTo(UNDONE);
        assertThat(overrideStudentContext.getState()).isEqualTo(CANCEL);
    }

    @Test
    <N>void shouldDoSequentialCommand_TransferNestedCommandResult() {
        int parameter = 112;
        Input<Integer> inputParameter = Input.of(parameter);
        command = spy(new FakeSequentialCommand(studentCommand, courseCommand, actionExecutor));
        command.putToNest(studentCommand);
        command.putToNest(courseCommand);
        command.putToNest(doubleCommand);
        command.putToNest(booleanCommand);
        command.putToNest(intCommand);
        wrapAllCommands();
        allowRealPrepareContextBase(inputParameter);
        allowRealPrepareContextExtra(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        checkRedefinedPrepareCommandContext(macroContext);
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
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        AtomicBoolean firstTime = new AtomicBoolean(true);
        RootCommand<?> unWrappedInt = ((SequentialMacroCommand.Chained<?>) intCommand).unWrap();
        RootCommand<?> unWrappedStudent = ((SequentialMacroCommand.Chained<?>) studentCommand).unWrap();
        RootCommand<?> unWrappedCourse = ((SequentialMacroCommand.Chained<?>) courseCommand).unWrap();
        wrapper.getNestedContexts().forEach(context -> {
            Context<N> nestedContext = (Context<N>) context;
            RootCommand<N> current = nestedContext.getCommand();
            RootCommand<N> unWrappedCurrent = (RootCommand<N>) ((SequentialMacroCommand.Chained<?>) current).unWrap();
            verify(current).doAsNestedCommand(eq(command), eq(context), any(Context.StateChangedListener.class));
            verify(unWrappedCurrent).doAsNestedCommand(eq(command), eq(context), any(Context.StateChangedListener.class));
            if (unWrappedCurrent != unWrappedInt) {
                verify((SequentialMacroCommand.Chained<?>) current).transferResultTo(eq(command), any(), any(Context.class));
            }
            if (firstTime.get()) {
                firstTime.compareAndSet(false, true);
            } else {
                verify(command).transferPreviousExecuteDoResult(eq(current), any(Optional.class), eq(context));
            }
            if (unWrappedCurrent != unWrappedStudent && unWrappedCurrent != unWrappedCourse) {
                verify(unWrappedCurrent).doCommand(nestedContext);
            }
            assertThat(context.isDone()).isTrue();
            assertThat(context.getResult()).isPresent();
        });
        verify(command).transferPreviousExecuteDoResult(any(StudentCommand.class), any(), any());
        verify(command).transferPreviousExecuteDoResult(any(CourseCommand.class), any(), any());
        verify(command, times(2)).transferPreviousExecuteDoResult(any(RootCommand.class), any(), any());
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

        allowRealNestedCommandExecution(doubleCommand);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        RootCommand<T> pureBooleanCommand = booleanCommand instanceof SequentialMacroCommand.Chained<?> nestedWrapper ?
                (RootCommand<T>) nestedWrapper.unWrap() : (RootCommand<T>) booleanCommand;
        doThrow(UnableExecuteCommandException.class).when(pureBooleanCommand).doCommand(any(Context.class));
        AtomicInteger counter = new AtomicInteger(0);
        Context.StateChangedListener listener = spy(new ContextStateChangedListener(counter));
        doCallRealMethod().when(pureBooleanCommand).doAsNestedCommand(eq(command), any(Context.class), eq(listener));

        command.executeNested(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isEqualTo(1);
        // check doubleCommand behaviour
        checkRegularNestedCommandExecution(doubleCommand, wrapper.getNestedContexts().pop(), listener);
        // check booleanCommand behaviour
        Context<T> nestedContext = (Context<T>) wrapper.getNestedContexts().pop();
        assertThat(nestedContext.isFailed()).isTrue();
        assertThat(nestedContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        verify(pureBooleanCommand).doAsNestedCommand(command, nestedContext, listener);
        verify(command).doNestedCommand(pureBooleanCommand, nestedContext, listener);
        verify(command, never()).transferPreviousExecuteDoResult(eq(pureBooleanCommand), any(), eq(nestedContext));
        verify(listener).stateChanged(nestedContext, READY, FAIL);
        // check intCommand behaviour
        nestedContext = (Context<T>) wrapper.getNestedContexts().pop();
        assertThat(nestedContext.getState()).isEqualTo(CANCEL);
        verify(intCommand, never()).doAsNestedCommand(command, nestedContext, listener);
        verify(command, never()).doNestedCommand((RootCommand<T>)intCommand, nestedContext, listener);
        verify(command, never()).transferPreviousExecuteDoResult(eq(intCommand), any(), eq(nestedContext));
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
        RootCommand<N> pureDoubleCommand = doubleCommand instanceof SequentialMacroCommand.Chained<?> nestedWrapper ?
                (RootCommand<N>) nestedWrapper.unWrap() : (RootCommand<N>) doubleCommand;
        doThrow(UnableExecuteCommandException.class).when(pureDoubleCommand).doCommand(any(Context.class));
        Context.StateChangedListener listener = spy(new ContextStateChangedListener(counter));
        doCallRealMethod().when(pureDoubleCommand).doAsNestedCommand(eq(command), any(Context.class), any(Context.StateChangedListener.class));

        command.executeNested(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isZero();
        Context<N> nestedContext = (Context<N>) wrapper.getNestedContexts().pop();
        // check doubleCommand behaviour
        assertThat(nestedContext.isFailed()).isTrue();
        assertThat(nestedContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        verify(doubleCommand).doAsNestedCommand(command, nestedContext, listener);
        verify(command).doNestedCommand(pureDoubleCommand, nestedContext, listener);
        verify(listener).stateChanged(nestedContext, READY, FAIL);
        // check booleanCommand behaviour
        nestedContext = (Context<N>) wrapper.getNestedContexts().pop();
        verify(booleanCommand, never()).doAsNestedCommand(command, nestedContext, listener);
        assertThat(nestedContext.getState()).isEqualTo(CANCEL);
        verify(listener).stateChanged(nestedContext, READY, CANCEL);
        // check intCommand behaviour
        nestedContext = (Context<N>) wrapper.getNestedContexts().pop();
        verify(intCommand, never()).doAsNestedCommand(command, nestedContext, listener);
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
        allowRealNestedCommandExecutionBase();
        command.doCommand(macroContext);
        Deque<Context<?>> nestedUndoneContexts = macroContext.<Deque<Context<?>>>getUndoParameter().value();
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);
        allowRealNestedCommandRollbackBase();

        Deque<Context<?>> rollbackResults = command.rollbackNestedDone(Input.of(nestedUndoneContexts));

        assertThat(nestedUndoneContexts).hasSameSizeAs(rollbackResults);
        int size = nestedUndoneContexts.size();
        List<Context<?>> params = nestedUndoneContexts.stream().toList();
        List<Context<?>> undone = rollbackResults.stream().toList();
        // check revers
        IntStream.range(0, size).forEach(i -> assertThat(undone.get(i)).isEqualTo(params.get(size - i - 1)));
        // check contexts states
        rollbackResults.forEach(context -> {
            var nestedCommand = context.getCommand();
            nestedCommand = nestedCommand instanceof SequentialMacroCommand.Chained<?> wrapped ? wrapped.unWrap() : nestedCommand;
            verify(nestedCommand).undoAsNestedCommand(command, context);
            verify(command).undoNestedCommand(nestedCommand, context);
            assertThat(context.getState()).isEqualTo(UNDONE);
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
        allowRealNestedCommandExecutionBase();
        command.doCommand(macroContext);
        Deque<Context<?>> nestedUndoneContexts = macroContext.<Deque<Context<?>>>getUndoParameter().value();
        RootCommand<?> pureDoubleCommand = doubleCommand instanceof SequentialMacroCommand.Chained<?> nestedWrapper ?
                nestedWrapper.unWrap() : doubleCommand;
        RootCommand<?> pureBoolCommand = booleanCommand instanceof SequentialMacroCommand.Chained<?> nestedWrapper ?
                nestedWrapper.unWrap() : booleanCommand;
        RootCommand<?> pureIntCommand = intCommand instanceof SequentialMacroCommand.Chained<?> nestedWrapper ?
                nestedWrapper.unWrap() : intCommand;
        doThrow(UnableExecuteCommandException.class).when(pureDoubleCommand).undoCommand(any(Context.class));
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);
        allowRealNestedCommandRollbackBase();

        Deque<Context<?>> rollbackResults = command.rollbackNestedDone(Input.of(nestedUndoneContexts));

        assertThat(nestedUndoneContexts).hasSameSizeAs(rollbackResults);
        int size = nestedUndoneContexts.size();
        List<Context<?>> params = nestedUndoneContexts.stream().toList();
        List<Context<?>> undone = rollbackResults.stream().toList();
        // check revers
        IntStream.range(0, size).forEach(i -> assertThat(undone.get(i)).isEqualTo(params.get(size - i - 1)));
        // check contexts order and states
        Context<?> nestedContext;
        nestedContext = rollbackResults.pop();
        verify(intCommand).undoAsNestedCommand(command, nestedContext);
        verify(command).undoNestedCommand(pureIntCommand, nestedContext);
        assertThat(nestedContext.getState()).isEqualTo(UNDONE);
        nestedContext = rollbackResults.pop();
        verify(booleanCommand).undoAsNestedCommand(command, nestedContext);
        verify(command).undoNestedCommand(pureBoolCommand, nestedContext);
        assertThat(nestedContext.getState()).isEqualTo(UNDONE);
        nestedContext = rollbackResults.pop();
        verify(command).undoNestedCommand(pureDoubleCommand, nestedContext);
        verify(doubleCommand).undoAsNestedCommand(command, nestedContext);
        assertThat(nestedContext.getState()).isEqualTo(FAIL);
        assertThat(nestedContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
    }

    static class FakeSequentialCommand extends SequentialMacroCommand<Double> {
        static CommandContext<Double> overrideStudentContext;
        static CommandContext<Double> overrideCourseContext;
        private final Logger logger = LoggerFactory.getLogger(FakeSequentialCommand.class);

        public FakeSequentialCommand(StudentCommand<?> student, CourseCommand<?> course, ActionExecutor actionExecutor) {
            super(actionExecutor);
            overrideStudentContext = CommandContext.<Double>builder().command((RootCommand<Double>) wrap(student)).state(INIT).build();
            overrideCourseContext = CommandContext.<Double>builder().command((RootCommand<Double>) wrap(course)).state(INIT).build();
        }

        @Override
        public Logger getLog() {
            return logger;
        }

        @Override
        public String getId() {
            return "sequential-fake-command";
        }

        /**
         * To detach command result data from persistence layer
         *
         * @param result result data to detach
         * @return detached result data
         * @see RootCommand#afterExecuteDo(Context)
         */
        @Override
        public Double detachedResult(Double result) {
            return result;
        }

        /**
         * To get mapper for business-message-payload
         *
         * @return mapper instance
         * @see BusinessMessagePayloadMapper
         */
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
        public <N> void doNestedCommand(StudentCommand<N> command, Context<N> doContext, Context.StateChangedListener stateListener) {
            doContext.addStateListener(stateListener);
            doContext.setState(WORK);
            ((Context<Double>) doContext).setResult(100.0);
            doContext.removeStateListener(stateListener);
        }

        @Override
        public <N> void doNestedCommand(CourseCommand<N> command, Context<N> doContext, Context.StateChangedListener stateListener) {
            doContext.addStateListener(stateListener);
            doContext.setState(WORK);
            ((Context<Double>) doContext).setResult(200.0);
            doContext.removeStateListener(stateListener);
        }

        @Override
        public Context<?> undoNestedCommand(StudentCommand<?> command, Context<?> undoContext) {
            undoContext.setState(CANCEL);
            return undoContext;
        }

        @Override
        public <S, T> void transferPreviousExecuteDoResult(final StudentCommand<?> command,
                                                           final S result, final Context<T> target) {
            getLog().info("Transfer student-command '{}' result:{} to {}", command, result, target);
            if (target instanceof CommandContext<T> commandContext) {
                commandContext.setRedoParameter(Input.of((Double)result + 1000));
            } else {
                throw new InvalidParameterTypeException(CommandContext.class.getSimpleName(), target.getClass().getSimpleName() );
            }
            getLog().info("Transferred student-command result context:{}", target);
        }

        /**
         * To prepare command for sequential macro-command
         *
         * @param command nested command to wrap
         * @return wrapped nested command
         * @see SequentialMacroCommand#putToNest(NestedCommand)
         */
        @Override
        public NestedCommand<?> wrap(NestedCommand<?> command) {
            if (command instanceof NestedCommand.InSequence) {
                // to avoid double wrapping
                return command;
            }
            if (command instanceof StudentCommand<?> studentCommand) {
                return spy(new StudentInSequenceCommand(studentCommand));
            }
            if (command instanceof CourseCommand<?> courseCommand) {
                return spy(new CourseInSequenceCommand(courseCommand));
            }
            return spy(new RootInSequenceCommand(command));
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

    private <T> void checkRegularNestedCommandExecution(RootCommand<?> nestedCommand,
                                                        @NonNull Context<T> nestedContext,
                                                        Context.StateChangedListener listener,
                                                        boolean lastNestedCommand) {
        assertThat(nestedContext.isDone()).isTrue();
        T doResult = nestedContext.getResult().orElseThrow();
        if (nestedCommand instanceof SequentialMacroCommand.Chained<?> inSequenceCommand) {
            nestedCommand = inSequenceCommand.unWrap();
        }
        verify(nestedCommand).doAsNestedCommand(command, nestedContext, listener);
        verify(command).doNestedCommand((RootCommand<T>) nestedCommand, nestedContext, listener);
        if (lastNestedCommand) {
            verify(command, never()).transferPreviousExecuteDoResult(eq(nestedCommand), any(), any(Context.class));
        } else {
            verify(command).transferPreviousExecuteDoResult(eq(nestedCommand), eq(doResult), any(Context.class));
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

    private void allowRealPrepareContextExtra(Input<?> parameter) {
        doCallRealMethod().when(studentCommand).acceptPreparedContext(command, parameter);
        doCallRealMethod().when(courseCommand).acceptPreparedContext(command, parameter);
    }

    private void allowRealNestedCommandExecution(RootCommand<?> nested) {
        if (nested instanceof SequentialMacroCommand.Chained<?> inSequenceCommand) {
            RootCommand<?> delegate = inSequenceCommand.unWrap();
            doCallRealMethod().when(delegate).doAsNestedCommand(eq(command), any(Context.class), any(Context.StateChangedListener.class));
        }
    }

    private void allowRealNestedCommandExecutionBase() {
        allowRealNestedCommandExecution(doubleCommand);
        allowRealNestedCommandExecution(booleanCommand);
        allowRealNestedCommandExecution(intCommand);
    }

    private void allowRealNestedCommandExecutionExtra() {
        allowRealNestedCommandExecution(studentCommand);
        allowRealNestedCommandExecution(courseCommand);
    }

    private void allowRealNestedCommandRollback(RootCommand<?> nested) {
        if (nested instanceof SequentialMacroCommand.Chained<?> inSequenceCommand) {
            nested = inSequenceCommand.unWrap();
        }
        doCallRealMethod().when(nested).undoAsNestedCommand(eq(command), any(Context.class));
    }

    private void allowRealNestedCommandRollbackBase() {
        allowRealNestedCommandRollback(doubleCommand);
        allowRealNestedCommandRollback(booleanCommand);
        allowRealNestedCommandRollback(intCommand);
    }

    private void allowRealNestedCommandRollbackExtra() {
        allowRealNestedCommandRollback(studentCommand);
        allowRealNestedCommandRollback(courseCommand);
    }

    private <T> void configureNestedRedoResult(RootCommand<?> nestedCommand, T result) {
        if (nestedCommand instanceof SequentialMacroCommand.Chained<?> inSequenceCommand) {
            nestedCommand = inSequenceCommand.unWrap();
        }
        doAnswer(invocationOnMock -> {
            Context<T> context = invocationOnMock.getArgument(0, Context.class);
            context.setState(WORK);
            context.setResult(result);
            return null;
        }).when(nestedCommand).doCommand(any(Context.class));
    }

    private void configureNestedUndoStatus(RootCommand<?> nestedCommand) {
        if (nestedCommand instanceof SequentialMacroCommand.Chained<?> inSequenceCommand) {
            nestedCommand = inSequenceCommand.unWrap();
        }
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
    private static class RootInSequenceCommand extends SequentialMacroCommand.Chained<RootCommand<?>> implements RootCommand<Void> {
        private final RootCommand<?> command;

        private RootInSequenceCommand(NestedCommand<?> command) {
            this.command = (RootCommand<?>) command;
        }

        @Override
        public RootCommand<?> unWrap() {
            return command;
        }

        @Override
        public <S, N> void transferResultTo(TransferResultVisitor visitor, S resultValue, Context<N> target) {
            visitor.transferPreviousExecuteDoResult(command, resultValue, target);
        }

        // root-command's stuff
        @Override
        public Logger getLog() {
            return command.getLog();
        }

        @Override
        public String getId() {
            return command.getId();
        }

        /**
         * To detach command result data from persistence layer
         *
         * @param result result data to detach
         * @return detached result data
         * @see RootCommand#afterExecuteDo(Context)
         */
        @Override
        public Void detachedResult(Void result) {
            return null;
        }

        /**
         * To get mapper for business-message-payload
         *
         * @return mapper instance
         * @see BusinessMessagePayloadMapper
         */
        @Override
        public BusinessMessagePayloadMapper getPayloadMapper() {
            return null;
        }

        @Override
        public void doAsNestedCommand(final NestedCommandExecutionVisitor visitor,
                                      final Context<?> context, final Context.StateChangedListener stateListener) {
            command.doAsNestedCommand(visitor, context, stateListener);
        }

        @Override
        public Context<?> undoAsNestedCommand(final NestedCommandExecutionVisitor visitor, final Context<?> context) {
            return command.undoAsNestedCommand(visitor, context);
        }
    }

    private static class StudentInSequenceCommand extends SequentialMacroCommand.Chained<StudentCommand<?>> implements StudentCommand<Void> {
        private final StudentCommand<?> command;

        private StudentInSequenceCommand(StudentCommand<?> command) {
            this.command = command;
        }

        @Override
        public StudentCommand<?> unWrap() {
            return command;
        }

        @Override
        public <S, N> void transferResultTo(TransferResultVisitor visitor, S resultValue, Context<N> target) {
            visitor.transferPreviousExecuteDoResult(command, resultValue, target);
        }

        // root-command's stuff
        @Override
        public Logger getLog() {
            return command.getLog();
        }

        @Override
        public String getId() {
            return command.getId();
        }

        /**
         * To get mapper for business-message-payload
         *
         * @return mapper instance
         * @see BusinessMessagePayloadMapper
         */
        @Override
        public BusinessMessagePayloadMapper getPayloadMapper() {
            return null;
        }

        @Override
        public void doAsNestedCommand(final NestedCommandExecutionVisitor visitor,
                                      final Context<?> context, final Context.StateChangedListener stateListener) {
            command.doAsNestedCommand(visitor, context, stateListener);
        }

        @Override
        public Context<?> undoAsNestedCommand(final NestedCommandExecutionVisitor visitor, final Context<?> context) {
            return command.undoAsNestedCommand(visitor, context);
        }
    }

    private static class CourseInSequenceCommand extends SequentialMacroCommand.Chained<CourseCommand<?>> implements CourseCommand<Void> {
        private final CourseCommand<?> command;

        private CourseInSequenceCommand(CourseCommand<?> command) {
            this.command = command;
        }

        @Override
        public CourseCommand<?> unWrap() {
            return command;
        }

        @Override
        public <S, N> void transferResultTo(TransferResultVisitor visitor, S resultValue, Context<N> target) {
            visitor.transferPreviousExecuteDoResult(command, resultValue, target);
        }

        // root-command's stuff
        @Override
        public Logger getLog() {
            return command.getLog();
        }

        @Override
        public String getId() {
            return command.getId();
        }

        /**
         * To get mapper for business-message-payload
         *
         * @return mapper instance
         * @see BusinessMessagePayloadMapper
         */
        @Override
        public BusinessMessagePayloadMapper getPayloadMapper() {
            return null;
        }

        @Override
        public void doAsNestedCommand(final NestedCommandExecutionVisitor visitor,
                                      final Context<?> context, final Context.StateChangedListener stateListener) {
            command.doAsNestedCommand(visitor, context, stateListener);
        }

        @Override
        public Context<?> undoAsNestedCommand(final NestedCommandExecutionVisitor visitor, final Context<?> context) {
            return command.undoAsNestedCommand(visitor, context);
        }
    }

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
