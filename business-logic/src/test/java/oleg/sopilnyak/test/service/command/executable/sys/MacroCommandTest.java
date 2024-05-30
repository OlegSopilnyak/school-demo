package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.SchoolCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import oleg.sopilnyak.test.service.exception.UnableExecuteCommandException;
import org.jetbrains.annotations.NotNull;
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
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static oleg.sopilnyak.test.service.command.executable.sys.MacroCommandTest.FakeMacroCommand.CONTEXT;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MacroCommandTest {
    @Spy
    @InjectMocks
    volatile FakeMacroCommand command;
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
        command.addToNest(doubleCommand);
        command.addToNest(booleanCommand);
        command.addToNest(intCommand);
    }

    @Test
    void checkIntegrity() {
        assertThat(command).isNotNull();
        assertThat(command.fromNest()).hasSize(3);
    }

    @Test
    <T> void shouldCreateMacroContext_Base() {
        int parameter = 100;
        allowRealPrepareContextBase(parameter);

        Context<Integer> macroContext = command.createContext(parameter);

        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        checkNestedContext(wrapper.getNestedContexts().pop(), doubleCommand, parameter);
        checkNestedContext(wrapper.getNestedContexts().pop(), booleanCommand, parameter);
        checkNestedContext(wrapper.getNestedContexts().pop(), intCommand, parameter);
        command.fromNest().forEach(nestedCommand -> {
            verify(nestedCommand).acceptPreparedContext(command, parameter);
            verify(command).prepareContext(nestedCommand, parameter);
            verify(nestedCommand).createContext(parameter);
        });
    }

    @Test
    <T> void shouldCreateMacroContext_StudentCommand() {
        int parameter = 115;
        command = spy(new FakeMacroCommand(studentCommand));
        command.addToNest(studentCommand);
        command.addToNest(doubleCommand);
        command.addToNest(booleanCommand);
        command.addToNest(intCommand);
        allowRealPrepareContextBase(parameter);
        allowRealPrepareContextExtra(parameter);

        Context<Integer> macroContext = command.createContext(parameter);

        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().stream()
                .filter(context -> context.getCommand() != studentCommand)
                .forEach(context -> {
                    SchoolCommand nestedCommand = context.getCommand();
                    assertThat(context.<Object>getRedoParameter()).isEqualTo(parameter);
                    verify(nestedCommand, atLeastOnce()).acceptPreparedContext(command, parameter);
                    verify(command).prepareContext(nestedCommand, parameter);
                    verify(nestedCommand).createContext(parameter);
                });
        // check studentCommand context
        assertThat(wrapper.getNestedContexts().getFirst()).isEqualTo(CONTEXT);
        assertThat(CONTEXT.getCommand()).isEqualTo(studentCommand);
        assertThat(CONTEXT.<Object>getRedoParameter()).isEqualTo(200);
        verify(studentCommand).acceptPreparedContext(command, parameter);
        verify(command).prepareContext(studentCommand, parameter);
        verify(studentCommand, never()).createContext(any());
    }

    @Test
    <T> void shouldCreateMacroContext_MacroCommand() {
        int parameter = 116;
        // prepare macro-command
        var command1 = mock(SchoolCommand.class);
        var command2 = mock(CourseCommand.class);
        var command3 = mock(StudentCommand.class);
        Context<T> studentContext = CommandContext.<T>builder().command(command3).state(INIT).build();
        var macroCommand = spy(new MacroCommand<SchoolCommand>() {
            @Override
            public String getId() {
                return "second-macro-command";
            }

            @Override
            public Logger getLog() {
                return LoggerFactory.getLogger("Second MacroCommand Logger");
            }

            @Override
            public Context<T> prepareContext(StudentCommand command, Object mainInput) {
                return prepareStudentContext();
            }

            Context<T> prepareStudentContext() {
                studentContext.setRedoParameter(200);
                return studentContext;
            }
        });
        macroCommand.addToNest(command1);
        macroCommand.addToNest(command2);
        macroCommand.addToNest(command3);
        allowRealPrepareContext(macroCommand, command1, parameter);
        allowRealPrepareContext(macroCommand, command2, parameter);
        allowRealAcceptPrepareContext(macroCommand, command3, parameter);
        // make root macro command
        command = spy(new FakeMacroCommand(studentCommand));
        command.addToNest(studentCommand);
        command.addToNest(doubleCommand);
        command.addToNest(booleanCommand);
        command.addToNest(intCommand);
        command.addToNest(macroCommand);
        // allow real methods for mocks
        allowRealPrepareContextBase(command, parameter);
        allowRealPrepareContextExtra(command, parameter);
        // macro-command isn't mock but spy

        Context<Integer> macroContext = command.createContext(parameter);

        MacroCommandParameter<T> wrapper = checkMainMacroCommandState(macroContext, parameter, macroCommand);
        // check contexts of nested macro command
        Context<T> macroCommandContext = wrapper.getNestedContexts().getLast();
        assertThat(macroCommandContext.isReady()).isTrue();
        MacroCommandParameter<T> macroWrapper = macroCommandContext.getRedoParameter();
        assertThat(macroWrapper.getInput()).isEqualTo(parameter);
        macroWrapper.getNestedContexts().forEach(context -> {
            assertThat(context.isReady()).isTrue();
            var nestedCommand = context.getCommand();
            verify(nestedCommand).acceptPreparedContext(macroCommand, parameter);
            if (nestedCommand instanceof StudentCommand) {
                assertThat(context.<Object>getRedoParameter()).isEqualTo(200);
            } else {
                assertThat(context.<Object>getRedoParameter()).isEqualTo(parameter);
                verify(nestedCommand).createContext(parameter);
            }
        });
        verify(macroCommand).prepareContext(command1, parameter);
        verify(macroCommand).prepareContext(command2, parameter);
        verify(macroCommand).prepareContext(command3, parameter);
        assertThat(macroWrapper.getNestedContexts().getLast()).isEqualTo(studentContext);
        verify(macroCommand).prepareStudentContext();
    }

    private <T> @NotNull MacroCommandParameter<T> checkMainMacroCommandState(Context<Integer> macroContext,
                                                                             Object parameter,
                                                                             MacroCommand<SchoolCommand> macroCommand) {
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().stream()
                .filter(context -> context.getCommand() != studentCommand)
                .filter(context -> context.getCommand() != macroCommand)
                .forEach(context -> {
                    assertThat(context.isReady()).isTrue();
                    SchoolCommand nestedCommand = context.getCommand();
                    assertThat(context.<Object>getRedoParameter()).isEqualTo(parameter);
                    verify(nestedCommand).acceptPreparedContext(command, parameter);
                    verify(command).prepareContext(nestedCommand, parameter);
                    verify(nestedCommand).createContext(parameter);
                });
        // check studentCommand context
        assertThat(wrapper.getNestedContexts().getFirst()).isEqualTo(CONTEXT);
        assertThat(CONTEXT.getCommand()).isEqualTo(studentCommand);
        assertThat(CONTEXT.<Object>getRedoParameter()).isEqualTo(200);
        verify(studentCommand).acceptPreparedContext(command, parameter);
        verify(command).prepareContext(studentCommand, parameter);
        verify(studentCommand, never()).createContext(any());
        verify(command).prepareStudentContext();
        return wrapper;
    }

    @Test
    <T> void shouldCreateMacroContext_WithEmptyNestedContexts() {
        int parameter = 101;
        doCallRealMethod().when(doubleCommand).acceptPreparedContext(command, parameter);
        doCallRealMethod().when(booleanCommand).acceptPreparedContext(command, parameter);
        doCallRealMethod().when(intCommand).acceptPreparedContext(command, parameter);

        Context<Integer> macroContext = command.createContext(parameter);

        assertThat(macroContext.isReady()).isTrue();
        command.fromNest().forEach(nestedCommand -> {
            verify(nestedCommand).acceptPreparedContext(command, parameter);
            verify(command).prepareContext(nestedCommand, parameter);
            verify(nestedCommand).createContext(parameter);
        });
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(context -> assertThat(context).isNull());
    }

    @Test
    void shouldNotCreateMacroContext_MacroContextExceptionThrown() {
        int parameter = 102;
        doThrow(new UnableExecuteCommandException(command.getId())).when(command).createContext(parameter);

        var exception = assertThrows(UnableExecuteCommandException.class, () -> command.createContext(parameter));

        assertThat(exception.getMessage()).isEqualTo("Cannot execute command 'fake-command'");
        command.fromNest().forEach(nested -> verify(nested, never()).acceptPreparedContext(eq(command), any()));
    }

    @Test
    void shouldNotCreateMacroContext_DoubleContextExceptionThrown() {
        int parameter = 103;
        allowRealAcceptPrepareContext(doubleCommand, parameter);
        doThrow(new UnableExecuteCommandException("double")).when(doubleCommand).createContext(parameter);

        var exception = assertThrows(UnableExecuteCommandException.class, () -> command.createContext(parameter));

        assertThat(exception.getMessage()).isEqualTo("Cannot execute command 'double'");
        // check doubleCommand
        verify(doubleCommand).acceptPreparedContext(command, parameter);
        verify(command).prepareContext(doubleCommand, parameter);
        verify(doubleCommand).createContext(parameter);
        // check other commands
        verify(booleanCommand, never()).acceptPreparedContext(eq(command), any());
        verify(intCommand, never()).acceptPreparedContext(eq(command), any());
    }

    @Test
    void shouldNotCreateMacroContext_IntContextExceptionThrown() {
        int parameter = 104;
        allowRealPrepareContext(doubleCommand, parameter);
        allowRealPrepareContext(booleanCommand, parameter);
        allowRealAcceptPrepareContext(intCommand, parameter);
        doThrow(new UnableExecuteCommandException("int")).when(intCommand).createContext(parameter);

        var exception = assertThrows(UnableExecuteCommandException.class, () -> command.createContext(parameter));

        assertThat(exception.getMessage()).isEqualTo("Cannot execute command 'int'");
        command.fromNest().forEach(nestedCommand -> {
            verify(nestedCommand).acceptPreparedContext(command, parameter);
            verify(command).prepareContext(nestedCommand, parameter);
            verify(nestedCommand).createContext(parameter);
        });
    }

    @Test
    <T> void shouldDoMacroCommandRedo_Base() {
        int parameter = 105;
        Object[] parameters = new Object[]{parameter * 100.0, true, parameter * 10};
        SchoolCommand[] nested = command.fromNest().toArray(SchoolCommand[]::new);
        allowRealPrepareContextBase(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        allowRealNestedCommandExecutionBase();
        IntStream.range(0, nested.length).forEach(i -> configureNestedRedoResult(nested[i], parameters[i]));

        command.doCommand(macroContext);

        assertThat(macroContext.isDone()).isTrue();
        verify(command).executeDo(macroContext);
        verify(command).doNestedCommands(eq(wrapper.getNestedContexts()), any(Context.StateChangedListener.class));
        wrapper.getNestedContexts().forEach(context -> {
            assertThat(context.isDone()).isTrue();
            SchoolCommand nestedCommand = context.getCommand();
            verify(nestedCommand).doAsNestedCommand(eq(command), eq(context), any(Context.StateChangedListener.class));
            verify(command).doNestedCommand(eq(nestedCommand), eq(context), any(Context.StateChangedListener.class));
            verify(nestedCommand).doCommand(context);
        });
        IntStream.range(0, nested.length).forEach(i -> assertCommandResult(nested[i], wrapper, parameters[i]));
        assertThat(macroContext.getResult().orElseThrow())
                .isEqualTo(wrapper.getNestedContexts().getLast().getResult().orElseThrow());
    }

    @Test
    <T> void shouldDoMacroCommandRedo_PlusStudentCommand() {
        int parameter = 125;
        Object[] parameters = new Object[]{parameter * 100.0, true, parameter * 10};
        command = spy(new FakeMacroCommand(studentCommand));
        allowRealPrepareContextBase(parameter);
        allowRealPrepareContextExtra(parameter);
        command.addToNest(studentCommand);
        command.addToNest(doubleCommand);
        command.addToNest(booleanCommand);
        command.addToNest(intCommand);
        SchoolCommand[] nested = command.fromNest().stream()
                .filter(schoolCommand -> schoolCommand != studentCommand)
                .toArray(SchoolCommand[]::new);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        IntStream.range(0, nested.length).forEach(i -> configureNestedRedoResult(nested[i], parameters[i]));
        allowRealNestedCommandExecutionBase();
        allowRealNestedCommandExecutionExtra();

        command.doCommand(macroContext);

        assertThat(macroContext.isDone()).isTrue();
        verify(command).executeDo(macroContext);
        verify(command).doNestedCommands(eq(wrapper.getNestedContexts()), any(Context.StateChangedListener.class));
        wrapper.getNestedContexts().stream()
                .filter(context -> context.getCommand() != studentCommand)
                .forEach(context -> {
                    assertThat(context.isDone()).isTrue();
                    SchoolCommand cmd = context.getCommand();
                    verify(cmd).doAsNestedCommand(eq(command), eq(context), any(Context.StateChangedListener.class));
                    verify(cmd).doCommand(context);
                });
        IntStream.range(0, nested.length).forEach(i -> assertCommandResult(nested[i], wrapper, parameters[i]));
        assertThat(macroContext.getResult().orElseThrow())
                .isEqualTo(wrapper.getNestedContexts().getLast().getResult().orElseThrow());
        verify(studentCommand, never()).doCommand(CONTEXT);
        assertCommandResult(studentCommand, wrapper, CONTEXT.getResult().orElseThrow());
        assertThat(wrapper.getNestedContexts().pop()).isEqualTo(CONTEXT);
    }

    @Test
    <T> void shouldDoMacroCommandRedo_MacroCommand() {
        int parameter = 126;
        // prepare nested macro-command
        var command1 = mock(SchoolCommand.class);
        var command2 = mock(CourseCommand.class);
        var command3 = mock(StudentCommand.class);
        final var courseResult = 100.0;
        var macroCommand = spy(new MacroCommand<SchoolCommand>() {
            @Override
            public String getId() {
                return "second-macro-command";
            }

            @Override
            public Logger getLog() {
                return LoggerFactory.getLogger("Second MacroCommand Logger");
            }

            @Override
            public <R> void doNestedCommand(CourseCommand command, Context<R> doContext, Context.StateChangedListener<R> stateListener) {
                doCourseCommand(doContext, stateListener);
            }

            <R> void doCourseCommand(Context<R> context, Context.StateChangedListener<R> listener) {
                context.addStateListener(listener);
                context.setState(WORK);
                context.setResult(courseResult);
                context.removeStateListener(listener);
            }
        });
        macroCommand.addToNest(command1);
        macroCommand.addToNest(command2);
        macroCommand.addToNest(command3);
        macroCommand.fromNest().forEach(mNested -> allowRealPrepareContext(macroCommand, mNested, parameter));
        // make root macro command
        command = spy(new FakeMacroCommand(studentCommand));
        command.addToNest(studentCommand);
        command.addToNest(doubleCommand);
        command.addToNest(booleanCommand);
        command.addToNest(intCommand);
        command.addToNest(macroCommand);
        // allow real methods for mocks
        allowRealPrepareContextBase(command, parameter);
        allowRealPrepareContextExtra(command, parameter);
        // macro-command isn't mock but spy
        Context<Number> macroContext = command.createContext(parameter);
        assertThat(macroContext.isReady()).isTrue();
        // configure execution of second macro-command nested commands
        macroCommand.fromNest().forEach(mNested -> allowRealNestedCommandExecution(macroCommand, mNested));
        configureNestedRedoResult(command1, parameter * 200.);
        configureNestedRedoResult(command3, parameter * 20);
        // configure execution of main macro-command
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 20);
        allowRealNestedCommandExecutionBase();
        allowRealNestedCommandExecutionExtra();

        command.doCommand(macroContext);

        // check main macro-command execution results
        assertThat(macroContext.isDone()).isTrue();
        verify(command).executeDo(macroContext);
        verify(command).doNestedCommands(eq(wrapper.getNestedContexts()), any(Context.StateChangedListener.class));
        wrapper.getNestedContexts().stream()
                .filter(context -> context.getCommand() != studentCommand)
                .forEach(context -> {
                    assertThat(context.isDone()).isTrue();
                    SchoolCommand cmd = context.getCommand();
                    verify(cmd).doAsNestedCommand(eq(command), eq(context), any(Context.StateChangedListener.class));
                    verify(cmd).doCommand(context);
                });
        assertThat(macroContext.getResult().orElseThrow())
                .isEqualTo(wrapper.getNestedContexts().getLast().getResult().orElseThrow());
        // student-command behavior when did doAsNestedCommand(...) method
        verify(studentCommand, never()).doCommand(any(Context.class));
        assertCommandResult(studentCommand, wrapper, CONTEXT.getResult().orElseThrow());
        assertThat(wrapper.getNestedContexts().pop()).isEqualTo(CONTEXT);
        // check nested macro-command execution results
        Context<T> macroCommandContext = wrapper.getNestedContexts().getLast();
        MacroCommandParameter<T> nestedWrapper = macroCommandContext.getRedoParameter();
        assertThat(macroCommandContext.getCommand()).isEqualTo(macroCommand);
        assertThat(nestedWrapper.getInput()).isEqualTo(parameter);
        nestedWrapper.getNestedContexts().forEach(context -> {
            assertThat(context.isDone()).isTrue();
            SchoolCommand nestedCommand = context.getCommand();
            verify(nestedCommand).doAsNestedCommand(eq(macroCommand), eq(context), any(Context.StateChangedListener.class));
            if (nestedCommand == command2) {
                // course-command
                verify(macroCommand).doCourseCommand(eq(context), any(Context.StateChangedListener.class));
                verify(nestedCommand, never()).doCommand(context);
            } else {
                verify(nestedCommand).doCommand(context);
            }
        });
        // check interaction between nested macro command and nested commands during Do of nested-macro-command
        verify(macroCommand).doNestedCommands(eq(nestedWrapper.getNestedContexts()), any(Context.StateChangedListener.class));
        verify(macroCommand).doNestedCommand(eq(command1), any(Context.class), any(Context.StateChangedListener.class));
        verify(macroCommand).doNestedCommand(eq(command2), any(Context.class), any(Context.StateChangedListener.class));
        verify(macroCommand).doNestedCommand(eq(command3), any(Context.class), any(Context.StateChangedListener.class));
        assertThat(macroCommandContext.getResult().orElseThrow())
                .isEqualTo(nestedWrapper.getNestedContexts().getLast().getResult().orElseThrow());
        assertThat(macroContext.getResult().orElseThrow()).isEqualTo(macroCommandContext.getResult().orElseThrow());
    }

    @Test
    <T> void shouldDontMacroCommandRedo_NoNestedContextsChain() {
        int parameter = 106;
        allowRealPrepareContextBase(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().clear();

        command.doCommand(macroContext);

        assertThat(macroContext.isFailed()).isTrue();
        assertThat(macroContext.getException()).isInstanceOf(NoSuchElementException.class);
        verify(command).executeDo(macroContext);
        verify(command).doNestedCommands(eq(wrapper.getNestedContexts()), any(Context.StateChangedListener.class));
    }

    @Test
    <T> void shouldDontMacroCommandRedo_NestedContextsAreNull() {
        int parameter = 107;
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(context -> assertThat(context).isNull());

        command.doCommand(macroContext);

        assertThat(macroContext.isFailed()).isTrue();
        assertThat(macroContext.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeDo(macroContext);
        verify(command).doNestedCommands(eq(wrapper.getNestedContexts()), any(Context.StateChangedListener.class));
    }

    @Test
    void shouldDontMacroCommandRedo_MacroContextThrows() {
        int parameter = 128;
        Context<Integer> macroContext = command.createContext(parameter);
        doThrow(UnableExecuteCommandException.class).when(command).executeDo(macroContext);

        Exception exception = assertThrows(UnableExecuteCommandException.class, () -> command.doCommand(macroContext));

        assertThat(exception).isInstanceOf(UnableExecuteCommandException.class);
        verify(command).executeDo(macroContext);
    }

    @Test
    <T> void shouldDontMacroCommandRedo_LastNestedContextThrows() {
        int parameter = 108;
        allowRealPrepareContextBase(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        allowRealNestedCommandExecutionBase();
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        allowRealNestedCommandRollback(doubleCommand);
        allowRealNestedCommandRollback(booleanCommand);
        doThrow(UnableExecuteCommandException.class).when(intCommand).doCommand(any(Context.class));

        command.doCommand(macroContext);

        assertThat(macroContext.isFailed()).isTrue();
        verify(command).executeDo(macroContext);
        Context<T> doubleContext = wrapper.getNestedContexts().getFirst();
        assertThat(doubleContext.isDone()).isTrue();
        assertThat(doubleContext.getResult().orElseThrow()).isEqualTo(parameter * 100.0);
        Context<T> intContext = wrapper.getNestedContexts().getLast();
        assertThat(intContext.isFailed()).isTrue();
        assertThat(intContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        assertThat(macroContext.getException()).isEqualTo(intContext.getException());
        verifyNestedCommandDoExecution(doubleCommand, doubleContext);
        verifyNestedCommandDoExecution(intCommand, intContext);
        verify(command).rollbackDoneContexts(any(Deque.class));
        verify(doubleCommand).undoCommand(wrapper.getNestedContexts().pop());
        verify(booleanCommand).undoCommand(wrapper.getNestedContexts().pop());
    }

    @Test
    <T> void shouldDontMacroCommandRedo_FirstNestedContextThrows() {
        int parameter = 109;
        allowRealPrepareContextBase(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        allowRealNestedCommandExecutionBase();
        doThrow(UnableExecuteCommandException.class).when(doubleCommand).doCommand(any(Context.class));
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        allowRealNestedCommandRollback(intCommand);
        allowRealNestedCommandRollback(booleanCommand);

        command.doCommand(macroContext);

        verify(command).executeDo(macroContext);
        assertThat(macroContext.isFailed()).isTrue();
        Context<T> doubleContext = wrapper.getNestedContexts().getFirst();
        assertThat(doubleContext.isFailed()).isTrue();
        assertThat(doubleContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        assertThat(macroContext.getException()).isEqualTo(doubleContext.getException());
        Context<T> intContext = wrapper.getNestedContexts().getLast();
        assertThat(intContext.isDone()).isTrue();
        assertThat(intContext.getResult().orElseThrow()).isEqualTo(parameter * 10);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));
        verifyNestedCommandDoExecution(doubleCommand, doubleContext);
        verifyNestedCommandDoExecution(intCommand, intContext);
        verify(command).rollbackDoneContexts(any(Deque.class));
        wrapper.getNestedContexts().pop();
        verify(booleanCommand).undoCommand(wrapper.getNestedContexts().pop());
        verify(intCommand).undoCommand(wrapper.getNestedContexts().pop());
    }

    @Test
    <T> void shouldDoMacroCommandUndo_BaseCommands() {
        int parameter = 110;
        allowRealPrepareContextBase(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        allowRealNestedCommandExecutionBase();
        command.doCommand(macroContext);
        assertThat(macroContext.getState()).isEqualTo(DONE);
        Deque<Context<T>> nestedDoneContexts = macroContext.getUndoParameter();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.getState()).isEqualTo(DONE));
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);
        allowRealNestedCommandRollbackBase();

        command.undoCommand(macroContext);

        assertThat(macroContext.getState()).isEqualTo(UNDONE);
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.getState()).isEqualTo(UNDONE));

        verify(command).executeUndo(macroContext);
        verify(command).rollbackDoneContexts(nestedDoneContexts);
        nestedDoneContexts.forEach(ctx -> verify(ctx.getCommand()).undoCommand(ctx));
    }

    @Test
    <T> void shouldDoMacroCommandUndo_PlusStudentCommand() {
        int parameter = 120;
        command = spy(new FakeMacroCommand(studentCommand));
        command.addToNest(studentCommand);
        command.addToNest(doubleCommand);
        command.addToNest(booleanCommand);
        command.addToNest(intCommand);
        allowRealPrepareContextBase(parameter);
        allowRealPrepareContextExtra(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        allowRealNestedCommandExecutionBase();
        allowRealNestedCommandExecutionExtra();
        command.doCommand(macroContext);
        assertThat(macroContext.getState()).isEqualTo(DONE);
        Deque<Context<T>> nestedDoneContexts = macroContext.getUndoParameter();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.getState()).isEqualTo(DONE));
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);
        allowRealNestedCommandRollbackBase();
        allowRealNestedCommandRollbackExtra();

        command.undoCommand(macroContext);

        assertThat(macroContext.getState()).isEqualTo(UNDONE);
        nestedDoneContexts.stream()
                .filter(context -> context.getCommand() != studentCommand)
                .forEach(context -> assertThat(context.getState()).isEqualTo(UNDONE));

        verify(command).executeUndo(macroContext);
        verify(command).rollbackDoneContexts(nestedDoneContexts);
        nestedDoneContexts.stream()
                .filter(context -> context.getCommand() != studentCommand)
                .forEach(context -> {
                    final var nestedCommand = context.getCommand();
                    verify(nestedCommand).undoAsNestedCommand(command, context);
                    verify(command).undoNestedCommand(nestedCommand, context);
                    verify(nestedCommand).undoCommand(context);
                });
        verify(studentCommand, never()).undoCommand(any(Context.class));
        assertThat(nestedDoneContexts.pop().getState()).isEqualTo(CANCEL);
    }

    @Test
    <T> void shouldDontMacroCommandUndo_MacroContextThrown() {
        int parameter = 111;
        allowRealPrepareContextBase(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        allowRealNestedCommandExecutionBase();
        command.doCommand(macroContext);
        assertThat(macroContext.isDone()).isTrue();
        Deque<Context<T>> nestedDoneContexts = macroContext.getUndoParameter();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.isDone()).isTrue());
        doThrow(UnableExecuteCommandException.class).when(command).executeUndo(macroContext);

        Exception exception = assertThrows(UnableExecuteCommandException.class, () -> command.undoCommand(macroContext));

        assertThat(exception).isInstanceOf(UnableExecuteCommandException.class);
        verify(command).executeUndo(macroContext);
        verify(command, never()).rollbackDoneContexts(nestedDoneContexts);
    }

    @Test
    <T> void shouldDontMacroCommandUndo_NestedContextThrown() {
        int parameter = 112;
        allowRealPrepareContextBase(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        allowRealNestedCommandExecutionBase();
        command.doCommand(macroContext);
        assertThat(macroContext.isDone()).isTrue();
        Deque<Context<T>> nestedDoneContexts = macroContext.getUndoParameter();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.isDone()).isTrue());
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(intCommand);
        doThrow(UnableExecuteCommandException.class).when(booleanCommand).undoCommand(any(Context.class));
        allowRealNestedCommandRollbackBase();

        command.undoCommand(macroContext);

        verify(command).executeUndo(macroContext);
        verify(command).rollbackDoneContexts(nestedDoneContexts);
        Context<T> current = nestedDoneContexts.pop();
        assertThat(current.getState()).isEqualTo(UNDONE);
        current = nestedDoneContexts.pop();
        assertThat(current.isFailed()).isTrue();
        assertThat(macroContext.isFailed()).isTrue();
        assertThat(macroContext.getException()).isEqualTo(current.getException());
        current = nestedDoneContexts.pop();
        assertThat(current.getState()).isEqualTo(UNDONE);
    }

    @Test
    void shouldPrepareMacroContext() {
        int parameter = 113;
        doCallRealMethod().when(doubleCommand).createContext(parameter);

        Context<Double> doubleContext = command.prepareContext(doubleCommand, parameter);

        assertThat(doubleContext).isNotNull();
        assertThat(doubleContext.getCommand()).isEqualTo(doubleCommand);
        assertThat(doubleContext.<Object>getRedoParameter()).isEqualTo(parameter);
        assertThat(doubleContext.getState()).isEqualTo(READY);

        verify(doubleCommand).createContext(parameter);
    }

    @Test
    void shouldNotPrepareMacroContext_ExceptionThrown() {
        int parameter = 114;
        when(doubleCommand.createContext(parameter)).thenThrow(new RuntimeException("cannot"));

        Exception exception = assertThrows(RuntimeException.class, () -> command.prepareContext(doubleCommand, parameter));

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).isEqualTo("cannot");
        verify(doubleCommand).createContext(parameter);
    }

    @Test
    <T> void shouldDoNestedContexts() {
        int parameter = 115;
        AtomicInteger counter = new AtomicInteger(0);
        allowRealPrepareContextBase(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        allowRealNestedCommandExecutionBase();
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        Context.StateChangedListener<T> listener = (context, previous, newOne) -> {
            if (newOne == DONE) {
                counter.incrementAndGet();
            }
        };

        command.doNestedCommands(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isEqualTo(3);
        wrapper.getNestedContexts().forEach(context -> {
            verify(context.getCommand()).doAsNestedCommand(command, context, listener);
            verify(command).doNestedCommand(context.getCommand(), context, listener);
            verify(context.getCommand()).doCommand(context);
            assertThat(context.isDone()).isTrue();
        });
    }

    @Test
    <T> void shouldNotDoNestedContexts_NestedRedoThrows() {
        int parameter = 116;
        AtomicInteger counter = new AtomicInteger(0);
        allowRealPrepareContextBase(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        allowRealNestedCommandExecutionBase();
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        doThrow(UnableExecuteCommandException.class).when(intCommand).doCommand(any(Context.class));
        Context.StateChangedListener<T> listener = (context, previous, newOne) -> {
            if (newOne == DONE) {
                counter.incrementAndGet();
            }
        };

        command.doNestedCommands(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isEqualTo(2);
        Context<T> current = wrapper.getNestedContexts().pop();
        verify(current.getCommand()).doCommand(current);
        assertThat(current.isDone()).isTrue();
        current = wrapper.getNestedContexts().pop();
        verify(current.getCommand()).doCommand(current);
        assertThat(current.isDone()).isTrue();
        current = wrapper.getNestedContexts().pop();
        verify(current.getCommand()).doCommand(current);
        assertThat(current.isFailed()).isTrue();
        assertThat(current.getException()).isInstanceOf(UnableExecuteCommandException.class);
    }

    @Test
    <T> void shouldDoNestedCommand() {
        int parameter = 117;
        AtomicInteger counter = new AtomicInteger(0);
        allowRealPrepareContext(doubleCommand, parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        allowRealNestedCommandExecution(doubleCommand);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        Context.StateChangedListener<T> listener = (context, previous, newOne) -> {
            if (newOne == DONE) {
                counter.incrementAndGet();
            }
        };
        Context<T> done = wrapper.getNestedContexts().getFirst();

        doubleCommand.doAsNestedCommand(command, done, listener);

        assertThat(counter.get()).isEqualTo(1);
        verify(done.getCommand()).doCommand(done);
        assertThat(done.getState()).isEqualTo(DONE);

    }

    @Test
    <T> void shouldNotDoNestedCommand_EmptyContext() {
        int parameter = 118;
        AtomicInteger counter = new AtomicInteger(0);
        Context<Integer> macroContext = command.createContext(parameter);
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        assertThat(wrapper.getNestedContexts().getFirst()).isNull();
        Context.StateChangedListener<T> listener = (context, previous, newOne) -> {
            if (newOne == DONE) {
                counter.incrementAndGet();
            }
        };

        assertThrows(NullPointerException.class, () -> command.doNestedCommand((SchoolCommand) null, null, listener));

        assertThat(counter.get()).isZero();
    }

    @Test
    <T> void shouldNotDoNestedCommand_NestedContextRedoThrows() {
        int parameter = 119;
        AtomicInteger counter = new AtomicInteger(0);
        allowRealPrepareContext(doubleCommand, parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        allowRealNestedCommandExecution(doubleCommand);
        doThrow(UnableExecuteCommandException.class).when(doubleCommand).doCommand(any(Context.class));
        Context.StateChangedListener<T> listener = (context, previous, newOne) -> {
            if (newOne == DONE) {
                counter.incrementAndGet();
            }
        };
        Context<T> done = wrapper.getNestedContexts().getFirst();

        doubleCommand.doAsNestedCommand(command, done, listener);

        assertThat(counter.get()).isZero();
        assertThat(done.getState()).isEqualTo(FAIL);
        assertThat(done.getException()).isInstanceOf(UnableExecuteCommandException.class);
    }

    static class FakeMacroCommand extends MacroCommand<SchoolCommand> {
        static Context<?> CONTEXT;
        Logger logger = LoggerFactory.getLogger(FakeMacroCommand.class);

        public FakeMacroCommand(StudentCommand student) {
            CONTEXT = CommandContext.<Double>builder().command(student).state(INIT).build();
        }

        @Override
        public <T> Context<T> prepareContext(StudentCommand command, Object mainInput) {
            return prepareStudentContext();
        }

        <T> Context<T> prepareStudentContext() {
            CONTEXT.setRedoParameter(200);
            return (Context<T>) CONTEXT;
        }

        @Override
        public <T> void doNestedCommand(StudentCommand command, Context<T> doContext, Context.StateChangedListener<T> stateListener) {
            doContext.addStateListener(stateListener);
            doContext.setState(WORK);
            doContext.setResult(100.0);
            doContext.removeStateListener(stateListener);
        }

        @Override
        public <T> Context<T> undoNestedCommand(StudentCommand command, Context<T> undoContext) {
            undoContext.setState(CANCEL);
            return undoContext;
        }

        @Override
        public Logger getLog() {
            return logger;
        }

        public String getId() {
            return "fake-command";
        }
    }

    // private methods
    private static <T> void assertCommandResult(SchoolCommand cmd, MacroCommandParameter<T> wrapper, Object expected) {
        var result = wrapper.getNestedContexts().stream()
                .filter(context -> context.getCommand().equals(cmd))
                .map(context -> context.getResult().orElseThrow())
                .findFirst().orElseThrow();
        assertThat(result).isEqualTo(expected);
    }

    private <T> void verifyNestedCommandDoExecution(SchoolCommand cmd, Context<T> context) {
        verify(cmd).doAsNestedCommand(eq(command), eq(context), any(Context.StateChangedListener.class));
        verify(command).doNestedCommand(eq(cmd), eq(context), any(Context.StateChangedListener.class));
        verify(cmd).doCommand(context);
    }

    private <T> void checkNestedContext(Context<T> nestedContext, SchoolCommand command, Object parameter) {
        assertThat(nestedContext).isNotNull();
        assertThat(nestedContext.isReady()).isTrue();
        assertThat(nestedContext.getCommand()).isEqualTo(command);
        assertThat(nestedContext.<Object>getRedoParameter()).isEqualTo(parameter);
    }

    private void allowRealPrepareContext(PrepareContextVisitor macro, SchoolCommand nested, Object parameter) {
        doCallRealMethod().when(nested).createContext(parameter);
        allowRealAcceptPrepareContext(macro, nested, parameter);
    }

    private void allowRealPrepareContext(SchoolCommand nested, Object parameter) {
        allowRealPrepareContext(command, nested, parameter);
    }

    private void allowRealAcceptPrepareContext(SchoolCommand nested, Object parameter) {
        allowRealAcceptPrepareContext(command, nested, parameter);
    }

    private void allowRealAcceptPrepareContext(PrepareContextVisitor macro, SchoolCommand nested, Object parameter) {
        doCallRealMethod().when(nested).acceptPreparedContext(macro, parameter);
    }

    private void allowRealPrepareContextBase(PrepareContextVisitor macro, Object parameter) {
        allowRealPrepareContext(macro, doubleCommand, parameter);
        allowRealPrepareContext(macro, booleanCommand, parameter);
        allowRealPrepareContext(macro, intCommand, parameter);
    }

    private void allowRealPrepareContextBase(Object parameter) {
        allowRealPrepareContextBase(command, parameter);
    }

    private void allowRealPrepareContextExtra(PrepareContextVisitor macro, Object parameter) {
        allowRealAcceptPrepareContext(macro, studentCommand, parameter);
    }

    private void allowRealPrepareContextExtra(Object parameter) {
        allowRealPrepareContextExtra(command, parameter);
    }

    private void allowRealNestedCommandExecution(NestedCommandExecutionVisitor macro, SchoolCommand nested) {
        doCallRealMethod().when(nested).doAsNestedCommand(eq(macro), any(Context.class), any(Context.StateChangedListener.class));
    }

    private void allowRealNestedCommandExecution(SchoolCommand nested) {
        allowRealNestedCommandExecution(command, nested);
    }

    private void allowRealNestedCommandExecutionBase() {
        allowRealNestedCommandExecution(doubleCommand);
        allowRealNestedCommandExecution(booleanCommand);
        doCallRealMethod().when(intCommand).doAsNestedCommand(eq(command), any(Context.class), any(Context.StateChangedListener.class));
    }

    private void allowRealNestedCommandExecutionExtra() {
        allowRealNestedCommandExecution(studentCommand);
    }


    private void allowRealNestedCommandRollback(SchoolCommand nested) {
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

    private <T> void configureNestedRedoResult(SchoolCommand nestedCommand, T result) {
        doAnswer(invocationOnMock -> {
            Context<T> context = invocationOnMock.getArgument(0, Context.class);
            context.setState(WORK);
            context.setResult(result);
            return null;
        }).when(nestedCommand).doCommand(any(Context.class));
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