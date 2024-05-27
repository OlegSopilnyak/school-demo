package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.NestedCommand;
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
import org.springframework.lang.NonNull;

import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommandTest.FakeSequentialCommand.STUDENT_CONTEXT;
import static oleg.sopilnyak.test.service.command.executable.sys.SequentialMacroCommandTest.FakeSequentialCommand.COURSE_CONTEXT;
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
    SchoolCommand doubleCommand;
    @Mock
    SchoolCommand booleanCommand;
    @Mock
    SchoolCommand intCommand;
    @Mock
    StudentCommand studentCommand;
    @Mock
    CourseCommand courseCommand;

    @BeforeEach
    void setUp() {
        command.add(doubleCommand);
        command.add(booleanCommand);
        command.add(intCommand);
    }

    @Test
    void checkSequentialCommandIntegrity() {
        assertThat(command).isNotNull();
        Deque<NestedCommand> commands = new LinkedList<>(command.commands());
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
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(macroContext.<Deque<Context<T>>>getUndoParameter()).hasSameSizeAs(wrapper.getNestedContexts());
        macroContext.<Deque<Context<T>>>getUndoParameter().forEach(context -> assertThat(context.isDone()).isTrue());
    }

    @Test
    <T> void shouldDoSequentialCommand_ExtraCommands() {
        int parameter = 121;
        command = spy(new FakeSequentialCommand(studentCommand, courseCommand));
        command.add(studentCommand);
        command.add(courseCommand);
        command.add(doubleCommand);
        command.add(booleanCommand);
        command.add(intCommand);
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
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(macroContext.<Deque<Context<T>>>getUndoParameter()).hasSameSizeAs(wrapper.getNestedContexts());
        macroContext.<Deque<Context<T>>>getUndoParameter().forEach(context -> assertThat(context.isDone()).isTrue());
        assertThat(STUDENT_CONTEXT.getResult()).contains(100.0);
        assertThat(COURSE_CONTEXT.getResult()).contains(200.0);
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
        verify(command).rollbackDoneContexts(nestedDoneContexts);
        nestedDoneContexts.forEach(context -> {
            final var nestedCommand = context.getCommand();
            verify(nestedCommand).undoAsNestedCommand(command, context);
            verify(command).undoNestedCommand(nestedCommand, context);
            verify(nestedCommand).undoCommand(context);
        });
    }

    @Test
    <T> void shouldUndoSequentialCommand_ExtraCommands() {
        int parameter = 132;
        command = spy(new FakeSequentialCommand(studentCommand, courseCommand));
        command.add(studentCommand);
        command.add(courseCommand);
        command.add(doubleCommand);
        command.add(booleanCommand);
        command.add(intCommand);
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
                .filter(context -> context != STUDENT_CONTEXT)
                .forEach(context -> assertThat(context.getState()).isEqualTo(UNDONE));
        verify(command).executeUndo(macroContext);
        verify(command).rollbackDoneContexts(nestedDoneContexts);
        nestedDoneContexts.stream()
                .filter(context -> context != STUDENT_CONTEXT)
                .filter(context -> context != COURSE_CONTEXT)
                .forEach(context -> {
                    final var nestedCommand = context.getCommand();
                    verify(nestedCommand).undoAsNestedCommand(command, context);
                    verify(command).undoNestedCommand(nestedCommand, context);
                    verify(nestedCommand).undoCommand(context);
                });
        assertThat(COURSE_CONTEXT.getState()).isEqualTo(UNDONE);
        assertThat(STUDENT_CONTEXT.getState()).isEqualTo(CANCEL);
    }

    @Test
    <T> void shouldDoSequentialCommand_TransferNestedCommandResult() {
        int parameter = 112;
        command = spy(new FakeSequentialCommand(studentCommand, courseCommand));
        command.add(studentCommand);
        command.add(courseCommand);
        command.add(doubleCommand);
        command.add(booleanCommand);
        command.add(intCommand);
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
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        AtomicBoolean firstTime = new AtomicBoolean(true);
        wrapper.getNestedContexts().forEach(context -> {
            final var current = context.getCommand();
            verify(current).doAsNestedCommand(eq(command), eq(context), any(Context.StateChangedListener.class));
            if (current != intCommand) {
                verify(current).transferResultTo(eq(command), any(), any(Context.class));
            }
            if (firstTime.get()) {
                firstTime.compareAndSet(false, true);
            } else {
                verify(command).transferPreviousExecuteDoResult(eq(current), any(Optional.class), eq(context));
            }
            if (current != studentCommand && current != courseCommand) {
                verify(current).doCommand(context);
            }
            assertThat(context.isDone()).isTrue();
            assertThat(context.getResult()).isPresent();
        });
        verify(command).transferPreviousExecuteDoResult(any(StudentCommand.class), any(), any());
        verify(command).transferPreviousExecuteDoResult(any(CourseCommand.class), any(), any());
        verify(command, times(2)).transferPreviousExecuteDoResult(any(SchoolCommand.class), any(), any());
        checkNestedCommandResultTransfer();
        wrapper.getNestedContexts().stream()
                .filter(context -> context != COURSE_CONTEXT)
                .filter(context -> context != STUDENT_CONTEXT)
                .forEach(context -> assertThat(context.<Object>getRedoParameter()).isEqualTo(parameter));
        assertThat(macroContext.<Deque<Context<T>>>getUndoParameter()).hasSameSizeAs(wrapper.getNestedContexts());
        macroContext.<Deque<Context<T>>>getUndoParameter().forEach(context -> assertThat(context.isDone()).isTrue());
    }

    private static void checkNestedCommandResultTransfer() {
        Double studentResult = STUDENT_CONTEXT.getResult().orElseThrow() + 1000;
        Double courseParameter = COURSE_CONTEXT.getRedoParameter();
        assertThat(courseParameter).isEqualTo(studentResult);
    }

    @Test
    <T> void shouldNotDoSomeNestedRedo_NestedRedoThrown() {
        int parameter = 101;
        AtomicInteger counter = new AtomicInteger(0);
        allowRealPrepareContextBase(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.isReady()).isTrue());
        allowRealNestedCommandExecution(doubleCommand);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        doThrow(UnableExecuteCommandException.class).when(booleanCommand).doCommand(any(Context.class));
        Context.StateChangedListener<T> listener = spy(new ContextStateChangedListener<>(counter));
        doCallRealMethod().when(booleanCommand).doAsNestedCommand(eq(command), any(Context.class), eq(listener));

        command.doNestedCommands(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isEqualTo(1);
        // check doubleCommand behaviour
        checkRegularNestedCommandExecution(doubleCommand, wrapper.getNestedContexts().pop(), listener);
        // check booleanCommand behaviour
        Context<T> nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.isFailed()).isTrue();
        assertThat(nestedContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        verify(booleanCommand).doAsNestedCommand(command, nestedContext, listener);
        verify(command).doNestedCommand(booleanCommand, nestedContext, listener);
        verify(command, never()).transferPreviousExecuteDoResult(eq(booleanCommand), any(), eq(nestedContext));
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
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.isReady()).isTrue());
        doThrow(UnableExecuteCommandException.class).when(doubleCommand).doCommand(any(Context.class));
        Context.StateChangedListener<T> listener = spy(new ContextStateChangedListener<>(counter));
        doCallRealMethod().when(doubleCommand).doAsNestedCommand(eq(command), any(Context.class), any(Context.StateChangedListener.class));

        command.doNestedCommands(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isZero();
        Context<T> nestedContext;
        // check doubleCommand behaviour
        nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.isFailed()).isTrue();
        assertThat(nestedContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        verify(doubleCommand).doAsNestedCommand(command, nestedContext, listener);
        verify(command).doNestedCommand(doubleCommand, nestedContext, listener);
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
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
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

        Deque<Context<T>> rollbackResults = command.rollbackDoneContexts(nestedUndoneContexts);

        assertThat(nestedUndoneContexts).hasSameSizeAs(rollbackResults);
        int size = nestedUndoneContexts.size();
        List<Context<T>> params = nestedUndoneContexts.stream().toList();
        List<Context<T>> undone = rollbackResults.stream().toList();
        // check revers
        IntStream.range(0, size).forEach(i -> assertThat(undone.get(i)).isEqualTo(params.get(size - i - 1)));
        // check contexts states
        rollbackResults.forEach(context -> {
            SchoolCommand nestedCommand = context.getCommand();
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
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.getState()).isEqualTo(READY));
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        allowRealNestedCommandExecutionBase();
        command.doCommand(macroContext);
        Deque<Context<T>> nestedUndoneContexts = macroContext.getUndoParameter();
        doThrow(UnableExecuteCommandException.class).when(doubleCommand).undoCommand(any(Context.class));
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);
        allowRealNestedCommandRollbackBase();

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
        verify(intCommand).undoAsNestedCommand(command, nestedContext);
        verify(command).undoNestedCommand(intCommand, nestedContext);
        assertThat(nestedContext.getState()).isEqualTo(UNDONE);
        nestedContext = rollbackResults.pop();
        verify(booleanCommand).undoAsNestedCommand(command, nestedContext);
        verify(command).undoNestedCommand(booleanCommand, nestedContext);
        assertThat(nestedContext.getState()).isEqualTo(UNDONE);
        nestedContext = rollbackResults.pop();
        verify(command).undoNestedCommand(doubleCommand, nestedContext);
        verify(doubleCommand).undoAsNestedCommand(command, nestedContext);
        assertThat(nestedContext.getState()).isEqualTo(FAIL);
        assertThat(nestedContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
    }

    static class FakeSequentialCommand extends SequentialMacroCommand {
        static Context<Double> STUDENT_CONTEXT;
        static Context<Double> COURSE_CONTEXT;
        private final Logger logger = LoggerFactory.getLogger(FakeSequentialCommand.class);

        public FakeSequentialCommand(StudentCommand student, CourseCommand course) {
            STUDENT_CONTEXT = CommandContext.<Double>builder().command(student).state(INIT).build();
            COURSE_CONTEXT = CommandContext.<Double>builder().command(course).state(INIT).build();
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
        public <T> Context<T> prepareContext(StudentCommand command, Object mainInput) {
            return prepareStudentContext();
        }

        @Override
        public <T> Context<T> prepareContext(CourseCommand command, Object mainInput) {
            return prepareCourseContext();
        }

        @Override
        public <T> void doNestedCommand(StudentCommand command, Context<T> doContext, Context.StateChangedListener<T> stateListener) {
            doContext.addStateListener(stateListener);
            doContext.setState(WORK);
            doContext.setResult(100.0);
            doContext.removeStateListener(stateListener);
        }

        @Override
        public <T> void doNestedCommand(CourseCommand command, Context<T> doContext, Context.StateChangedListener<T> stateListener) {
            doContext.addStateListener(stateListener);
            doContext.setState(WORK);
            doContext.setResult(200.0);
            doContext.removeStateListener(stateListener);
        }

        @Override
        public <T> Context<T> undoNestedCommand(StudentCommand command, Context<T> undoContext) {
            undoContext.setState(CANCEL);
            return undoContext;
        }

        @Override
        public <S, T> void transferPreviousExecuteDoResult(StudentCommand command, S result, Context<T> target) {
            getLog().info("Transfer student-command '{}' result:{} to {}", command, result, target);
            target.setRedoParameter(((Double) result) + 1000);
            getLog().info("Transferred student-command result context:{}", target);
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
    private static <T> void checkRedefinedPrepareCommandContext(Context<Integer> macroContext) {
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        Deque<Context<T>> contexts = new LinkedList<>(wrapper.getNestedContexts());
        assertThat(contexts.pop()).isEqualTo(STUDENT_CONTEXT);
        assertThat(contexts.pop()).isEqualTo(COURSE_CONTEXT);
    }

    private <T> void checkRegularNestedCommandExecution(SchoolCommand nestedCommand,
                                                        Context<T> nestedContext,
                                                        Context.StateChangedListener<T> listener) {
        checkRegularNestedCommandExecution(nestedCommand, nestedContext, listener, false);
    }

    private <T> void checkRegularNestedCommandExecution(SchoolCommand nestedCommand,
                                                        @NonNull Context<T> nestedContext,
                                                        Context.StateChangedListener<T> listener,
                                                        boolean lastNestedCommand) {
        assertThat(nestedContext.isDone()).isTrue();
        T doResult = nestedContext.getResult().orElseThrow();
        verify(nestedCommand).doAsNestedCommand(command, nestedContext, listener);
        verify(command).doNestedCommand(nestedCommand, nestedContext, listener);
        if (lastNestedCommand) {
            verify(command, never()).transferPreviousExecuteDoResult(eq(intCommand), any(), any(Context.class));
        } else {
            verify(command).transferPreviousExecuteDoResult(eq(nestedCommand), eq(doResult), any(Context.class));
        }
        verify(listener).stateChanged(nestedContext, READY, WORK);
        verify(listener).stateChanged(nestedContext, WORK, DONE);
    }

    private void allowRealPrepareContext(NestedCommand nested, Object parameter) {
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

    private void allowRealNestedCommandExecution(NestedCommand nested) {
        doCallRealMethod().when(nested).transferResultTo(eq(command), any(), any(Context.class));
        doCallRealMethod().when(nested).doAsNestedCommand(eq(command), any(Context.class), any(Context.StateChangedListener.class));
    }

    private void allowRealNestedCommandExecutionBase() {
        allowRealNestedCommandExecution(doubleCommand);
        allowRealNestedCommandExecution(booleanCommand);
        doCallRealMethod().when(intCommand).doAsNestedCommand(eq(command), any(Context.class), any(Context.StateChangedListener.class));
    }

    private void allowRealNestedCommandExecutionExtra() {
        allowRealNestedCommandExecution(studentCommand);
        allowRealNestedCommandExecution(courseCommand);
    }

    private void allowRealNestedCommandRollback(NestedCommand nested) {
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

    private <T> void configureNestedRedoResult(SchoolCommand nestedCommand, T result) {
        doAnswer(invocationOnMock -> {
            Context<T> context = invocationOnMock.getArgument(0, Context.class);
            context.setState(WORK);
            context.setResult(result);
            return null;
        }).when(nestedCommand).doCommand(any(Context.class));
    }

    private <T> void configureNestedUndoStatus(SchoolCommand nestedCommand) {
        doAnswer(invocationOnMock -> {
            Context<T> context = invocationOnMock.getArgument(0, Context.class);
            context.setState(WORK);
            context.setState(UNDONE);
            return null;
        }).when(nestedCommand).undoCommand(any(Context.class));
    }

    private static void verifyNestedCommandContextPreparation(FakeSequentialCommand command,
                                                              SchoolCommand nestedCommand,
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
        STUDENT_CONTEXT.setRedoParameter(200);
        return (Context<T>) STUDENT_CONTEXT;
    }

    private static <T> Context<T> prepareCourseContext() {
        COURSE_CONTEXT.setRedoParameter(300);
        return (Context<T>) COURSE_CONTEXT;
    }

}
