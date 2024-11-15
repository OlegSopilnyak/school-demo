package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.TransferResultVisitor;
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
import org.springframework.lang.NonNull;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommandTest.FakeSequentialCommand.overridedCourseContext;
import static oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommandTest.FakeSequentialCommand.overridedStudentContext;
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
    volatile RootCommand doubleCommand;
    @Mock
    volatile RootCommand booleanCommand;
    @Mock
    volatile RootCommand intCommand;
    @Mock
    volatile StudentCommand studentCommand;
    @Mock
    volatile CourseCommand courseCommand;

    @BeforeEach
    void setUp() {
        command.addToNest(doubleCommand);
        command.addToNest(booleanCommand);
        command.addToNest(intCommand);
        wrapBaseCommands();
    }

    @Test
    void checkSequentialCommandIntegrity() {
        assertThat(command).isNotNull();
        Deque<NestedCommand> commands = new LinkedList<>(command.fromNest());
        assertThat(commands.pop()).isEqualTo(doubleCommand);
        assertThat(commands.pop()).isEqualTo(booleanCommand);
        assertThat(commands.pop()).isEqualTo(intCommand);
    }

    @Test
    <T> void shouldDoAllNestedCommand() {
        int parameter = 100;
        allowRealPrepareContextBase(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.isReady()).isTrue());
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        AtomicInteger counter = new AtomicInteger(0);
        Context.StateChangedListener listener = spy(new ContextStateChangedListener(counter));
        allowRealNestedCommandExecutionBase();

        command.doNestedCommands(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isEqualTo(command.fromNest().size());
        // check contexts states
        checkRegularNestedCommandExecution(doubleCommand, wrapper.getNestedContexts().pop(), listener);
        checkRegularNestedCommandExecution(booleanCommand, wrapper.getNestedContexts().pop(), listener);
        checkRegularNestedCommandExecution(intCommand, wrapper.getNestedContexts().pop(), listener, true);
    }

    @Test
    <T> void shouldDoSequentialCommand_BaseCommands() {
        int parameter = 111;
        allowRealPrepareContextBase(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        verifyNestedCommandContextPreparation(command, doubleCommand, parameter);
        verifyNestedCommandContextPreparation(command, booleanCommand, parameter);
        verifyNestedCommandContextPreparation(command, intCommand, parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        allowRealNestedCommandExecutionBase();

        // doing command with macro-context
        command.doCommand(macroContext);

        // after do check
        verify(command).executeDo(macroContext);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(macroContext.isDone()).isTrue();
        MacroCommandParameter wrapper = macroContext.getRedoParameter();
        assertThat(macroContext.<Deque<Context<T>>>getUndoParameter()).hasSameSizeAs(wrapper.getNestedContexts());
        macroContext.<Deque<Context<T>>>getUndoParameter().forEach(context -> assertThat(context.isDone()).isTrue());
        RootCommand pureDoubleCommand = doubleCommand instanceof SequentialMacroCommand.Chained<?> nested ? nested.unWrap() : doubleCommand;
        RootCommand pureBooleanCommand = booleanCommand instanceof SequentialMacroCommand.Chained<?> nested ? nested.unWrap() : booleanCommand;
        verify(command).transferPreviousExecuteDoResult(eq(pureDoubleCommand), any(), any(Context.class));
        verify(command).transferPreviousExecuteDoResult(eq(pureBooleanCommand), any(), any(Context.class));
    }

    @Test
    <T> void shouldDoSequentialCommand_ExtraCommands() {
        int parameter = 121;
        command = spy(new FakeSequentialCommand(studentCommand, courseCommand));
        command.addToNest(studentCommand);
        command.addToNest(courseCommand);
        command.addToNest(doubleCommand);
        command.addToNest(booleanCommand);
        command.addToNest(intCommand);
        wrapAllCommands();
        allowRealPrepareContextBase(parameter);
        allowRealPrepareContextExtra(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        verifyNestedCommandContextPreparation(command, studentCommand, parameter);
        verifyNestedCommandContextPreparation(command, courseCommand, parameter);
        verifyNestedCommandContextPreparation(command, doubleCommand, parameter);
        verifyNestedCommandContextPreparation(command, booleanCommand, parameter);
        verifyNestedCommandContextPreparation(command, intCommand, parameter);
        checkRedefinedPrepareCommandContext(macroContext);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        allowRealNestedCommandExecutionBase();
        allowRealNestedCommandExecutionExtra();

        // doing command with macro-context
        command.doCommand(macroContext);

        // after do check
        verify(command).executeDo(macroContext);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(macroContext.isDone()).isTrue();
        MacroCommandParameter wrapper = macroContext.getRedoParameter();
        assertThat(macroContext.<Deque<Context<T>>>getUndoParameter()).hasSameSizeAs(wrapper.getNestedContexts());
        macroContext.<Deque<Context<T>>>getUndoParameter().forEach(context -> assertThat(context.isDone()).isTrue());
        assertThat(overridedStudentContext.getResult()).contains(100.0);
        assertThat(overridedCourseContext.getResult()).contains(200.0);
    }

    @Test
    <T> void shouldUndoSequentialCommand_BaseCommands() {
        int parameter = 131;
        allowRealPrepareContextBase(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        verifyNestedCommandContextPreparation(command, doubleCommand, parameter);
        verifyNestedCommandContextPreparation(command, booleanCommand, parameter);
        verifyNestedCommandContextPreparation(command, intCommand, parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        allowRealNestedCommandExecutionBase();
        command.doCommand(macroContext);
        assertThat(macroContext.isDone()).isTrue();
        Deque<Context<T>> nestedDoneContexts = macroContext.getUndoParameter();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.isDone()).isTrue());
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);
        allowRealNestedCommandRollbackBase();

        command.undoCommand(macroContext);

        assertThat(macroContext.getState()).isEqualTo(UNDONE);
        nestedDoneContexts.forEach(context -> assertThat(context.getState()).isEqualTo(UNDONE));

        verify(command).executeUndo(macroContext);
        verify(command).undoNestedCommands(nestedDoneContexts);
        nestedDoneContexts.forEach(context -> {
            var nestedCommand = context.getCommand();
//            nestedCommand = nestedCommand instanceof SequentialMacroCommand.Chained<?> wrapped ? wrapped.unWrap() : nestedCommand;
            verify(nestedCommand).undoAsNestedCommand(command, context);
            verify(command).undoNestedCommand(nestedCommand, context);
            verify(nestedCommand).undoCommand(context);
        });
    }

    @Test
    <T> void shouldUndoSequentialCommand_ExtraCommands() {
        int parameter = 132;
        command = spy(new FakeSequentialCommand(studentCommand, courseCommand));
        command.addToNest(studentCommand);
        command.addToNest(courseCommand);
        command.addToNest(doubleCommand);
        command.addToNest(booleanCommand);
        command.addToNest(intCommand);
        wrapAllCommands();
        allowRealPrepareContextBase(parameter);
        allowRealPrepareContextExtra(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        verifyNestedCommandContextPreparation(command, studentCommand, parameter);
        verifyNestedCommandContextPreparation(command, courseCommand, parameter);
        verifyNestedCommandContextPreparation(command, doubleCommand, parameter);
        verifyNestedCommandContextPreparation(command, booleanCommand, parameter);
        verifyNestedCommandContextPreparation(command, intCommand, parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        allowRealNestedCommandExecutionBase();
        allowRealNestedCommandExecutionExtra();
        command.doCommand(macroContext);
        assertThat(macroContext.isDone()).isTrue();
        Deque<Context<T>> nestedDoneContexts = macroContext.getUndoParameter();
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
                .filter(context -> context != overridedStudentContext)
                .forEach(context -> assertThat(context.getState()).isEqualTo(UNDONE));
        verify(command).executeUndo(macroContext);
        verify(command).undoNestedCommands(nestedDoneContexts);
        nestedDoneContexts.stream()
                .filter(context -> context != overridedStudentContext)
                .filter(context -> context != overridedCourseContext)
                .forEach(context -> {
                    var nestedCommand = context.getCommand();
//                    nestedCommand = nestedCommand instanceof SequentialMacroCommand.Chained<?> wrapped ? wrapped.unWrap() : nestedCommand;
                    verify(nestedCommand).undoAsNestedCommand(command, context);
                    verify(command).undoNestedCommand(nestedCommand, context);
                    verify(nestedCommand).undoCommand(context);
                });
        assertThat(overridedCourseContext.getState()).isEqualTo(UNDONE);
        assertThat(overridedStudentContext.getState()).isEqualTo(CANCEL);
    }

    @Test
    <T> void shouldDoSequentialCommand_TransferNestedCommandResult() {
        int parameter = 112;
        command = spy(new FakeSequentialCommand(studentCommand, courseCommand));
        command.addToNest(studentCommand);
        command.addToNest(courseCommand);
        command.addToNest(doubleCommand);
        command.addToNest(booleanCommand);
        command.addToNest(intCommand);
        wrapAllCommands();
        allowRealPrepareContextBase(parameter);
        allowRealPrepareContextExtra(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        checkRedefinedPrepareCommandContext(macroContext);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        allowRealNestedCommandExecutionBase();
        allowRealNestedCommandExecutionExtra();

        // doing command with macro-context
        command.doCommand(macroContext);

        // after do check
        verify(command).executeDo(macroContext);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(macroContext.isDone()).isTrue();
        MacroCommandParameter wrapper = macroContext.getRedoParameter();
        AtomicBoolean firstTime = new AtomicBoolean(true);
        RootCommand unWrappedInt = ((SequentialMacroCommand.Chained<?>) intCommand).unWrap();
        RootCommand unWrappedStudent = ((SequentialMacroCommand.Chained<?>) studentCommand).unWrap();
        RootCommand unWrappedCourse = ((SequentialMacroCommand.Chained<?>) courseCommand).unWrap();
        wrapper.getNestedContexts().forEach(context -> {
            var current = context.getCommand();
            RootCommand unWrappedCurrent = ((SequentialMacroCommand.Chained<?>) current).unWrap();
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
                verify(unWrappedCurrent).doCommand(context);
            }
            assertThat(context.isDone()).isTrue();
            assertThat(context.getResult()).isPresent();
        });
        verify(command).transferPreviousExecuteDoResult(any(StudentCommand.class), any(), any());
        verify(command).transferPreviousExecuteDoResult(any(CourseCommand.class), any(), any());
        verify(command, times(2)).transferPreviousExecuteDoResult(any(RootCommand.class), any(), any());
        checkNestedCommandResultTransfer();
        wrapper.getNestedContexts().stream()
                .filter(context -> context != overridedCourseContext)
                .filter(context -> context != overridedStudentContext)
                .forEach(context -> assertThat(context.<Object>getRedoParameter()).isEqualTo(parameter));
        assertThat(macroContext.<Deque<Context<T>>>getUndoParameter()).hasSameSizeAs(wrapper.getNestedContexts());
        macroContext.<Deque<Context<T>>>getUndoParameter().forEach(context -> assertThat(context.isDone()).isTrue());
    }

    @Test
    <T> void shouldNotDoSomeNestedRedo_NestedRedoThrown() {
        int parameter = 101;
        allowRealPrepareContextBase(parameter);

        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.isReady()).isTrue());

        allowRealNestedCommandExecution(doubleCommand);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        RootCommand pureBooleanCommand = booleanCommand instanceof SequentialMacroCommand.Chained<?> nestedWrapper ?
                nestedWrapper.unWrap() : booleanCommand;
        doThrow(UnableExecuteCommandException.class).when(pureBooleanCommand).doCommand(any(Context.class));
        AtomicInteger counter = new AtomicInteger(0);
        Context.StateChangedListener listener = spy(new ContextStateChangedListener(counter));
        doCallRealMethod().when(pureBooleanCommand).doAsNestedCommand(eq(command), any(Context.class), eq(listener));

        command.doNestedCommands(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isEqualTo(1);
        // check doubleCommand behaviour
        checkRegularNestedCommandExecution(doubleCommand, wrapper.getNestedContexts().pop(), listener);
        // check booleanCommand behaviour
        Context<?> nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.isFailed()).isTrue();
        assertThat(nestedContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        verify(pureBooleanCommand).doAsNestedCommand(command, nestedContext, listener);
        verify(command).doNestedCommand(pureBooleanCommand, nestedContext, listener);
        verify(command, never()).transferPreviousExecuteDoResult(eq(pureBooleanCommand), any(), eq(nestedContext));
        verify(listener).stateChanged(nestedContext, READY, FAIL);
        // check intCommand behaviour
        nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.getState()).isEqualTo(CANCEL);
        verify(intCommand, never()).doAsNestedCommand(command, nestedContext, listener);
        verify(command, never()).doNestedCommand(intCommand, nestedContext, listener);
        verify(command, never()).transferPreviousExecuteDoResult(eq(intCommand), any(), eq(nestedContext));
        verify(listener).stateChanged(nestedContext, READY, CANCEL);
    }

    @Test
    <T> void shouldNotDoAllNestedRedo_NestedRedoThrown() {
        int parameter = 102;
        AtomicInteger counter = new AtomicInteger(0);
        allowRealPrepareContextBase(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.isReady()).isTrue());
        RootCommand pureDoubleCommand = doubleCommand instanceof SequentialMacroCommand.Chained<?> nestedWrapper ?
                nestedWrapper.unWrap() : doubleCommand;
        doThrow(UnableExecuteCommandException.class).when(pureDoubleCommand).doCommand(any(Context.class));
        Context.StateChangedListener listener = spy(new ContextStateChangedListener(counter));
        doCallRealMethod().when(pureDoubleCommand).doAsNestedCommand(eq(command), any(Context.class), any(Context.StateChangedListener.class));

        command.doNestedCommands(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isZero();
        Context<?> nestedContext = wrapper.getNestedContexts().pop();
        // check doubleCommand behaviour
        assertThat(nestedContext.isFailed()).isTrue();
        assertThat(nestedContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        verify(doubleCommand).doAsNestedCommand(command, nestedContext, listener);
        verify(command).doNestedCommand(pureDoubleCommand, nestedContext, listener);
        verify(listener).stateChanged(nestedContext, READY, FAIL);
        // check booleanCommand behaviour
        nestedContext = wrapper.getNestedContexts().pop();
        verify(booleanCommand, never()).doAsNestedCommand(command, nestedContext, listener);
        assertThat(nestedContext.getState()).isEqualTo(CANCEL);
        verify(listener).stateChanged(nestedContext, READY, CANCEL);
        // check intCommand behaviour
        nestedContext = wrapper.getNestedContexts().pop();
        verify(intCommand, never()).doAsNestedCommand(command, nestedContext, listener);
        assertThat(nestedContext.getState()).isEqualTo(CANCEL);
        verify(listener).stateChanged(nestedContext, READY, CANCEL);
    }

    @Test
    <T> void shouldRollbackAllNestedDoneContexts_BaseCommands() {
        int parameter = 103;
        allowRealPrepareContextBase(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        MacroCommandParameter wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.getState()).isEqualTo(READY));
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        allowRealNestedCommandExecutionBase();
        command.doCommand(macroContext);
        Deque<Context<T>> nestedUndoneContexts = macroContext.getUndoParameter();
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);
        allowRealNestedCommandRollbackBase();

        Deque<Context<T>> rollbackResults = command.undoNestedCommands(nestedUndoneContexts);

        assertThat(nestedUndoneContexts).hasSameSizeAs(rollbackResults);
        int size = nestedUndoneContexts.size();
        List<Context<T>> params = nestedUndoneContexts.stream().toList();
        List<Context<T>> undone = rollbackResults.stream().toList();
        // check revers
        IntStream.range(0, size).forEach(i -> assertThat(undone.get(i)).isEqualTo(params.get(size - i - 1)));
        // check contexts states
        rollbackResults.forEach(context -> {
            var nestedCommand = context.getCommand();
//            nestedCommand = nestedCommand instanceof SequentialMacroCommand.Chained<?> wrapped ? wrapped.unWrap() : nestedCommand;
            verify(nestedCommand).undoAsNestedCommand(command, context);
            verify(command).undoNestedCommand(nestedCommand, context);
            assertThat(context.getState()).isEqualTo(UNDONE);
        });
    }

    @Test
    <T> void shouldNotRollbackSomeNestedDoneContexts_NestedUndoThrown() {
        int parameter = 104;
        allowRealPrepareContextBase(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        MacroCommandParameter wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.getState()).isEqualTo(READY));
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        allowRealNestedCommandExecutionBase();
        command.doCommand(macroContext);
        Deque<Context<T>> nestedUndoneContexts = macroContext.getUndoParameter();
        RootCommand pureDoubleCommand = doubleCommand instanceof SequentialMacroCommand.Chained<?> nestedWrapper ?
                nestedWrapper.unWrap() : doubleCommand;
        RootCommand pureBoolCommand = booleanCommand instanceof SequentialMacroCommand.Chained<?> nestedWrapper ?
                nestedWrapper.unWrap() : booleanCommand;
        RootCommand pureIntCommand = intCommand instanceof SequentialMacroCommand.Chained<?> nestedWrapper ?
                nestedWrapper.unWrap() : intCommand;
        doThrow(UnableExecuteCommandException.class).when(pureDoubleCommand).undoCommand(any(Context.class));
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);
        allowRealNestedCommandRollbackBase();

        Deque<Context<T>> rollbackResults = command.undoNestedCommands(nestedUndoneContexts);

        assertThat(nestedUndoneContexts).hasSameSizeAs(rollbackResults);
        int size = nestedUndoneContexts.size();
        List<Context<T>> params = nestedUndoneContexts.stream().toList();
        List<Context<T>> undone = rollbackResults.stream().toList();
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

    static class FakeSequentialCommand extends SequentialMacroCommand {
        static Context<Double> overridedStudentContext;
        static Context<Double> overridedCourseContext;
        private final Logger logger = LoggerFactory.getLogger(FakeSequentialCommand.class);

        public FakeSequentialCommand(StudentCommand student, CourseCommand course) {
            overridedStudentContext = CommandContext.<Double>builder().command(wrap(student)).state(INIT).build();
            overridedCourseContext = CommandContext.<Double>builder().command(wrap(course)).state(INIT).build();
        }

        @Override
        public Logger getLog() {
            return logger;
        }

        @Override
        public String getId() {
            return "sequential-fake-command";
        }
//
//        @Override
//        public <T> Context<T> prepareContext(StudentCommand command, Object mainInput) {
//            return prepareStudentContext();
//        }
//
//        @Override
//        public <T> Context<T> prepareContext(CourseCommand command, Object mainInput) {
//            return prepareCourseContext();
//        }
//
//        @Override
//        public <T> void doNestedCommand(StudentCommand command, Context<T> doContext, Context.StateChangedListener stateListener) {
//            doContext.addStateListener(stateListener);
//            doContext.setState(WORK);
//            doContext.setResult(100.0);
//            doContext.removeStateListener(stateListener);
//        }
//
//        @Override
//        public <T> void doNestedCommand(CourseCommand command, Context<T> doContext, Context.StateChangedListener stateListener) {
//            doContext.addStateListener(stateListener);
//            doContext.setState(WORK);
//            doContext.setResult(200.0);
//            doContext.removeStateListener(stateListener);
//        }
//
//        @Override
//        public <T> Context<T> undoNestedCommand(StudentCommand command, Context<T> undoContext) {
//            undoContext.setState(CANCEL);
//            return undoContext;
//        }

        @Override
        public <S, T> void transferPreviousExecuteDoResult(StudentCommand command, S result, Context<T> target) {
            getLog().info("Transfer student-command '{}' result:{} to {}", command, result, target);
            target.setRedoParameter(((Double) result) + 1000);
            getLog().info("Transferred student-command result context:{}", target);
        }

        /**
         * To prepare command for sequential macro-command
         *
         * @param command nested command to wrap
         * @return wrapped nested command
         * @see SequentialMacroCommand#addToNest(NestedCommand)
         */
        @Override
        public RootCommand wrap(NestedCommand command) {
            if (command instanceof NestedCommand.InSequence) {
                // to avoid double wrapping
                return (RootCommand) command;
            }
//            if (command instanceof StudentCommand studentCommand) {
//                return spy(new StudentInSequenceCommand(studentCommand));
//            }
//            if (command instanceof CourseCommand courseCommand) {
//                return spy(new CourseInSequenceCommand(courseCommand));
//            }
//            return spy(new RootInSequenceCommand(command));

            return null;
        }
    }

    // private methods
    private static void checkNestedCommandResultTransfer() {
        Double studentResult = overridedStudentContext.getResult().orElseThrow() + 1000;
        Double courseParameter = overridedCourseContext.getRedoParameter();
        assertThat(courseParameter).isEqualTo(studentResult);
    }

    private static <T> void checkRedefinedPrepareCommandContext(Context<Integer> macroContext) {
        MacroCommandParameter wrapper = macroContext.getRedoParameter();
        Deque<Context<?>> contexts = new LinkedList<>(wrapper.getNestedContexts());
        assertThat(contexts.pop()).isEqualTo(overridedStudentContext);
        assertThat(contexts.pop()).isEqualTo(overridedCourseContext);
    }

    private <T> void checkRegularNestedCommandExecution(RootCommand nestedCommand, Context<T> nestedContext, Context.StateChangedListener listener) {
        checkRegularNestedCommandExecution(nestedCommand, nestedContext, listener, false);
    }

    private <T> void checkRegularNestedCommandExecution(RootCommand nestedCommand,
                                                        @NonNull Context<T> nestedContext,
                                                        Context.StateChangedListener listener,
                                                        boolean lastNestedCommand) {
        assertThat(nestedContext.isDone()).isTrue();
        T doResult = nestedContext.getResult().orElseThrow();
        if (nestedCommand instanceof SequentialMacroCommand.Chained<?> inSequenceCommand) {
            nestedCommand = inSequenceCommand.unWrap();
        }
        verify(nestedCommand).doAsNestedCommand(command, nestedContext, listener);
        verify(command).doNestedCommand(nestedCommand, nestedContext, listener);
        if (lastNestedCommand) {
            verify(command, never()).transferPreviousExecuteDoResult(eq(nestedCommand), any(), any(Context.class));
        } else {
            verify(command).transferPreviousExecuteDoResult(eq(nestedCommand), eq(doResult), any(Context.class));
        }
        verify(listener).stateChanged(nestedContext, READY, WORK);
        verify(listener).stateChanged(nestedContext, WORK, DONE);
    }

    private void allowRealPrepareContext(RootCommand nested, Object parameter) {
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
        doCallRealMethod().when(courseCommand).acceptPreparedContext(command, parameter);
    }

    private void allowRealNestedCommandExecution(RootCommand nested) {
        if (nested instanceof SequentialMacroCommand.Chained<?> inSequenceCommand) {
            RootCommand delegate = inSequenceCommand.unWrap();
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

    private void allowRealNestedCommandRollback(RootCommand nested) {
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

    private <T> void configureNestedRedoResult(RootCommand nestedCommand, T result) {
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

    private <T> void configureNestedUndoStatus(RootCommand nestedCommand) {
        if (nestedCommand instanceof SequentialMacroCommand.Chained<?> inSequenceCommand) {
            nestedCommand = inSequenceCommand.unWrap();
        }
        doAnswer(invocationOnMock -> {
            Context<T> context = invocationOnMock.getArgument(0, Context.class);
            context.setState(WORK);
            context.setState(UNDONE);
            return null;
        }).when(nestedCommand).undoCommand(any(Context.class));
    }

    private static void verifyNestedCommandContextPreparation(FakeSequentialCommand command,
                                                              RootCommand nestedCommand,
                                                              Object parameter) {
        verify(nestedCommand).acceptPreparedContext(command, parameter);
        verify(command).prepareContext(nestedCommand, parameter);
    }

    private static void verifyNestedCommandContextPreparation(FakeSequentialCommand command,
                                                              StudentCommand nestedCommand,
                                                              Object parameter) {
        verify(nestedCommand).acceptPreparedContext(command, parameter);
        verify(command).prepareContext(nestedCommand, parameter);
    }

    private static void verifyNestedCommandContextPreparation(FakeSequentialCommand command,
                                                              CourseCommand nestedCommand,
                                                              Object parameter) {
        verify(nestedCommand).acceptPreparedContext(command, parameter);
        verify(command).prepareContext(nestedCommand, parameter);
    }

    private static <T> Context<T> prepareStudentContext() {
        overridedStudentContext.setRedoParameter(200);
        return (Context<T>) overridedStudentContext;
    }

    private static <T> Context<T> prepareCourseContext() {
        overridedCourseContext.setRedoParameter(300);
        return (Context<T>) overridedCourseContext;
    }

    private void wrapBaseCommands() {
        Deque<NestedCommand> commands = new LinkedList<>(command.fromNest());
        doubleCommand = (RootCommand) commands.pop();
        booleanCommand = (RootCommand) commands.pop();
        intCommand = (RootCommand) commands.pop();
    }

    private void wrapAllCommands() {
        Deque<NestedCommand> commands = new LinkedList<>(command.fromNest());
        studentCommand = (StudentCommand) commands.pop();
        courseCommand = (CourseCommand) commands.pop();
        doubleCommand = (RootCommand) commands.pop();
        booleanCommand = (RootCommand) commands.pop();
        intCommand = (RootCommand) commands.pop();
    }

    // inner classes
//    private static class RootInSequenceCommand extends SequentialMacroCommand.Chained implements RootCommand {
//        private final RootCommand command;
//
//        private RootInSequenceCommand(NestedCommand command) {
//            this.command = (RootCommand) command;
//        }
//
//        @Override
//        public RootCommand unWrap() {
//            return command;
//        }
//
//        @Override
//        public <S, T> void transferResultTo(TransferResultVisitor visitor, S resultValue, Context<T> target) {
//            visitor.transferPreviousExecuteDoResult(command, resultValue, target);
//        }
//    }
//
//    private static class StudentInSequenceCommand extends SequentialMacroCommand.Chained<StudentCommand> implements StudentCommand {
//        private final StudentCommand command;
//
//        private StudentInSequenceCommand(StudentCommand command) {
//            this.command = command;
//        }
//
//        @Override
//        public StudentCommand unWrap() {
//            return command;
//        }
//
//        @Override
//        public <S, T> void transferResultTo(TransferResultVisitor visitor, S resultValue, Context<T> target) {
//            visitor.transferPreviousExecuteDoResult(command, resultValue, target);
//        }
//    }

//    private static class CourseInSequenceCommand extends SequentialMacroCommand.Chained<CourseCommand> implements CourseCommand {
//        private final CourseCommand command;
//
//        private CourseInSequenceCommand(CourseCommand command) {
//            this.command = command;
//        }
//
//        @Override
//        public CourseCommand unWrap() {
//            return command;
//        }
//
//        @Override
//        public <S, T> void transferResultTo(TransferResultVisitor visitor, S resultValue, Context<T> target) {
//            visitor.transferPreviousExecuteDoResult(command, resultValue, target);
//        }
//    }

    static class ContextStateChangedListener implements Context.StateChangedListener {
        private final AtomicInteger counter;

        ContextStateChangedListener(AtomicInteger counter) {
            this.counter = counter;
        }

        @Override
        public void stateChanged(Context<?> context, Context.State previous, Context.State newOne) {
            if (newOne == DONE) {
                counter.incrementAndGet();
            }
        }

    }

}
