package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.CourseCommand;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
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
    FakeMacroCommand_RedefinedPrepareContext command;
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
        Deque<SchoolCommand> commands = new LinkedList<>(command.commands());
        assertThat(commands.pop()).isEqualTo(doubleCommand);
        assertThat(commands.pop()).isEqualTo(booleanCommand);
        assertThat(commands.pop()).isEqualTo(intCommand);
    }

    @Test
    <T> void shouldDoAllNestedCommand() {
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
        verify(command).transferPreviousExecuteDoResult(eq(nestedContext.getCommand()), eq(nestedContext.getResult()), any(Context.class));
        verify(listener).stateChanged(nestedContext, READY, WORK);
        verify(listener).stateChanged(nestedContext, WORK, DONE);
        nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.getState()).isEqualTo(DONE);
        verify(command).doNestedCommand(nestedContext, listener);
        verify(command).transferPreviousExecuteDoResult(eq(nestedContext.getCommand()), eq(nestedContext.getResult()), any(Context.class));
        verify(listener).stateChanged(nestedContext, READY, WORK);
        verify(listener).stateChanged(nestedContext, WORK, DONE);
        nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.getState()).isEqualTo(DONE);
        verify(command).doNestedCommand(nestedContext, listener);
        verify(command, never()).transferPreviousExecuteDoResult(eq(nestedContext.getCommand()), eq(nestedContext.getResult()), any(Context.class));
        verify(listener).stateChanged(nestedContext, READY, WORK);
        verify(listener).stateChanged(nestedContext, WORK, DONE);
    }

    @Test
    <T> void shouldDoSequentialCommand_PreparedNestedContexts() {
        int parameter = 111;
        command = spy(new FakeMacroCommand_RedefinedPrepareContext(studentCommand, courseCommand));
        command.add(studentCommand);
        command.add(courseCommand);
        command.add(doubleCommand);
        command.add(booleanCommand);
        command.add(intCommand);
        allowCreateRealContexts(parameter);
        allowCreateExtraRealContexts(parameter);
        configureNestedRedoResult(studentCommand, parameter * 200.0);
        configureNestedRedoResult(courseCommand, parameter * 300.0);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);

        Context<Integer> macroContext = command.createContext(parameter);

        verify(studentCommand).acceptPreparedContext(command, parameter);
        verify(courseCommand).acceptPreparedContext(command, parameter);
        verify(doubleCommand).acceptPreparedContext(command, parameter);
        verify(booleanCommand).acceptPreparedContext(command, parameter);
        verify(intCommand).acceptPreparedContext(command, parameter);
        verify(command).prepareContext(studentCommand, parameter);
        verify(command).prepareContext(courseCommand, parameter);
        verify(command).prepareContext(doubleCommand, parameter);
        verify(command).prepareContext(booleanCommand, parameter);
        verify(command).prepareContext(intCommand, parameter);
        CommandParameterWrapper<T> wrapper = macroContext.getRedoParameter();
        Deque<Context<T>> contexts = new LinkedList<>(wrapper.getNestedContexts());
        assertThat(contexts.pop()).isEqualTo(FakeMacroCommand_RedefinedPrepareContext.STUDENT_CONTEXT);
        assertThat(contexts.pop()).isEqualTo(FakeMacroCommand_RedefinedPrepareContext.COURSE_CONTEXT);
        contexts.forEach(context -> assertThat(context).isInstanceOf(CommandContext.class));

        // doing command with macro-context
        command.doCommand(macroContext);

        // after do check
        verify(command).executeDo(macroContext);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));
        assertThat(macroContext.isDone()).isTrue();
        AtomicBoolean firstTime = new AtomicBoolean(true);
        wrapper.getNestedContexts().forEach(context -> {
            if (firstTime.get()) {
                firstTime.compareAndSet(false, true);
            } else {
                verify(command).transferPreviousExecuteDoResult(any(SchoolCommand.class), any(Optional.class), eq(context));
            }
            verify(command).doNestedCommand(eq(context), any(Context.StateChangedListener.class));
            verify(context.getCommand()).doCommand(context);
            assertThat(context.isDone()).isTrue();
            assertThat(context.getResult()).isPresent();
        });
        verify(command, times(wrapper.getNestedContexts().size() - 1))
                .transferPreviousExecuteDoResult(any(SchoolCommand.class), any(), any());
        assertThat(macroContext.<Deque<Context<T>>>getUndoParameter()).hasSameSizeAs(wrapper.getNestedContexts());
        macroContext.<Deque<Context<T>>>getUndoParameter().forEach(context -> assertThat(context.isDone()).isTrue());
    }

    private static <T> Context<T> prepareStudentContext() {
        FakeMacroCommand_RedefinedPrepareContext.STUDENT_CONTEXT.setRedoParameter(200);
        return (Context<T>) FakeMacroCommand_RedefinedPrepareContext.STUDENT_CONTEXT;
    }

    private static <T> Context<T> prepareCourseContext() {
        FakeMacroCommand_RedefinedPrepareContext.COURSE_CONTEXT.setRedoParameter(300);
        return (Context<T>) FakeMacroCommand_RedefinedPrepareContext.COURSE_CONTEXT;
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
        verify(command).transferPreviousExecuteDoResult(eq(nestedContext.getCommand()), eq(nestedContext.getResult()), any(Context.class));
        verify(listener).stateChanged(nestedContext, READY, WORK);
        verify(listener).stateChanged(nestedContext, WORK, DONE);
        nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.getState()).isEqualTo(FAIL);
        assertThat(nestedContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        verify(command).doNestedCommand(nestedContext, listener);
        verify(command, never()).transferPreviousExecuteDoResult(eq(nestedContext.getCommand()), eq(nestedContext.getResult()), any(Context.class));
        verify(listener).stateChanged(nestedContext, READY, FAIL);
        nestedContext = wrapper.getNestedContexts().pop();
        assertThat(nestedContext.getState()).isEqualTo(CANCEL);
        verify(command, never()).doNestedCommand(nestedContext, listener);
        verify(command, never()).transferPreviousExecuteDoResult(eq(nestedContext.getCommand()), eq(nestedContext.getResult()), any(Context.class));
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

    static class FakeMacroCommand_RedefinedPrepareContext extends SequentialMacroCommand {
        static Context<?> STUDENT_CONTEXT;
        static Context<?> COURSE_CONTEXT;
        private final Logger logger = LoggerFactory.getLogger(FakeMacroCommand_RedefinedPrepareContext.class);

        public FakeMacroCommand_RedefinedPrepareContext(StudentCommand student, CourseCommand course) {
            STUDENT_CONTEXT = CommandContext.builder().command(student).state(INIT).build();
            COURSE_CONTEXT = CommandContext.builder().command(course).state(INIT).build();
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

        /**
         * To transfer result form previous command to current context
         *
         * @param previousCommand previous successfully executed command
         * @param previousResult  the result of previous command execution
         * @param targetContext   current command context to execute command's redo
         * @see SchoolCommand
         * @see Context
         * @see Optional
         */
        @Override
        protected <S, T> void transferPreviousExecuteDoResult(
                StudentCommand previousCommand,
                Optional<S> previousResult,
                Context<T> targetContext) {
            super.transferPreviousExecuteDoResult(previousCommand, previousResult, targetContext);
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

    private void allowCreateExtraRealContexts(Object parameter) {
        // visitor accept
        doCallRealMethod().when(studentCommand).acceptPreparedContext(command, parameter);
        doCallRealMethod().when(courseCommand).acceptPreparedContext(command, parameter);
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
}