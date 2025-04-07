package oleg.sopilnyak.test.service.command.executable.sys;

import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.io.parameter.MacroCommandParameter;
import oleg.sopilnyak.test.service.command.type.CourseCommand;
import oleg.sopilnyak.test.service.command.type.StudentCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.base.RootCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommand;
import oleg.sopilnyak.test.service.command.type.nested.NestedCommandExecutionVisitor;
import oleg.sopilnyak.test.service.command.type.nested.PrepareContextVisitor;
import oleg.sopilnyak.test.service.exception.CannotCreateCommandContextException;
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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static oleg.sopilnyak.test.service.command.executable.sys.MacroCommandTest.FakeMacroCommand.overridedStudentContext;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MacroCommandTest {
    @Spy
    @InjectMocks
    volatile FakeMacroCommand command;
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
        command.putToNest(doubleCommand);
        command.putToNest(booleanCommand);
        command.putToNest(intCommand);
    }

    @Test
    void checkIntegrity() {
        assertThat(command).isNotNull();
        assertThat(command.fromNest()).hasSize(3);
    }

    @Test
    void shouldCreateMacroContext_Base() {
        int parameter = 100;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContextBase(inputParameter);

        Context<Double> macroContext = command.createContext(inputParameter);

        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        checkNestedContext(wrapper.getNestedContexts().pop(), doubleCommand, inputParameter);
        checkNestedContext(wrapper.getNestedContexts().pop(), booleanCommand, inputParameter);
        checkNestedContext(wrapper.getNestedContexts().pop(), intCommand, inputParameter);
        command.fromNest().forEach(nestedCommand -> {
            verify(nestedCommand).acceptPreparedContext(command, inputParameter);
            if (nestedCommand instanceof RootCommand<?> rootCommand) {
                verify(rootCommand).createContext(inputParameter);
            } else {
                fail("NestedCommand not a RootCommand.");
            }
        });
        verify(command).prepareContext(doubleCommand, inputParameter);
        verify(command).prepareContext(booleanCommand, inputParameter);
        verify(command).prepareContext(intCommand, inputParameter);
    }

    @Test
    void shouldCreateMacroContext_StudentCommand() {
        int parameter = 115;
        Input<Integer> inputParameter = Input.of(parameter);
        command = spy(new FakeMacroCommand(studentCommand));
        command.putToNest(studentCommand);
        command.putToNest(doubleCommand);
        command.putToNest(booleanCommand);
        command.putToNest(intCommand);
        allowRealPrepareContextBase(inputParameter);
        allowRealPrepareContextExtra(inputParameter);

        Context<Double> macroContext = command.createContext(inputParameter);

        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        wrapper.getNestedContexts().stream()
                .filter(context -> context.getCommand() != studentCommand)
                .forEach(context -> {
                    RootCommand<?> nestedCommand = context.getCommand();
                    assertThat(context.getRedoParameter().value()).isEqualTo(parameter);
                    verify(nestedCommand, atLeastOnce()).acceptPreparedContext(command, inputParameter);
                    verify(command).prepareContext(nestedCommand, inputParameter);
                    verify(nestedCommand).createContext(inputParameter);
                });
        // check studentCommand context
        assertThat(wrapper.getNestedContexts().getFirst()).isEqualTo(overridedStudentContext);
        assertThat(overridedStudentContext.getCommand()).isEqualTo(studentCommand);
        assertThat(overridedStudentContext.getRedoParameter().value()).isEqualTo(200);
        verify(studentCommand).acceptPreparedContext(command, inputParameter);
        verify(command).prepareContext(studentCommand, inputParameter);
        verify(studentCommand, never()).createContext(any());
    }

    @Test
    void shouldCreateMacroContext_StudentAndMacroCommand() {
        int parameter = 116;
        Input<Integer> inputParameter = Input.of(parameter);
        // prepare macro-command
        RootCommand<?> command1 = mock(RootCommand.class);
        CourseCommand<?> command2 = mock(CourseCommand.class);
        StudentCommand<Number> command3 = mock(StudentCommand.class);
        Context<?> studentContext = CommandContext.<Number>builder().command(command3).state(INIT).build();
        var macroCommand = spy(new MacroCommand<Void>() {
            @Override
            public String getId() {
                return "second-macro-void-command";
            }

            @Override
            public Logger getLog() {
                return LoggerFactory.getLogger("Second MacroCommand Logger");
            }

            @Override
            public <N> Context<N> prepareContext(StudentCommand<N> command, Input<?> mainInput) {
                return prepareStudentContext();
            }

            <N> Context<N> prepareStudentContext() {
                if (studentContext instanceof CommandContext<?> commandContext) {
                    commandContext.setRedoParameter(Input.of(200));
                } else {
                    throw new RuntimeException("student-context is not a CommandContext");
                }
                return (Context<N>) studentContext;
            }
        });
        macroCommand.putToNest(command1);
        macroCommand.putToNest(command2);
        macroCommand.putToNest(command3);
        allowRealPrepareContext(macroCommand, command1, inputParameter);
        allowRealPrepareContext(macroCommand, command2, inputParameter);
        allowRealAcceptPrepareContext(macroCommand, command3, inputParameter);
        // make root macro command
        command = spy(new FakeMacroCommand(studentCommand));
        command.putToNest(studentCommand);
        command.putToNest(doubleCommand);
        command.putToNest(booleanCommand);
        command.putToNest(intCommand);
        command.putToNest(macroCommand);
        // allow real methods for mocks
        allowRealPrepareContextBase(command, inputParameter);
        allowRealPrepareContextExtra(command, inputParameter);
        // macro-command isn't mock but spy

        Context<Double> macroContext = command.createContext(inputParameter);

        MacroCommandParameter wrapper = checkMainMacroCommandState(macroContext, inputParameter, macroCommand);
        // check contexts of nested macro command
        Context<?> macroCommandContext = wrapper.getNestedContexts().getLast();
        assertThat(macroCommandContext.isReady()).isTrue();

        MacroCommandParameter macroCommandParameterWrapper = macroCommandContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(macroCommandParameterWrapper.getRootInput().value()).isSameAs(parameter);
        macroCommandParameterWrapper.getNestedContexts().forEach(context -> {
            assertThat(context.isReady()).isTrue();
            RootCommand<?> nestedCommand = context.getCommand();
            verify(nestedCommand).acceptPreparedContext(macroCommand, inputParameter);
            if (nestedCommand instanceof StudentCommand<?>) {
                assertThat(context.<Number>getRedoParameter().value()).isEqualTo(200);
            } else {
                assertThat(context.<Number>getRedoParameter().value()).isEqualTo(parameter);
                verify(nestedCommand).createContext(inputParameter);
            }
        });
        verify(macroCommand).prepareContext(command1, inputParameter);
        verify(macroCommand).prepareContext(command2, inputParameter);
        verify(macroCommand).prepareContext(command3, inputParameter);
        assertThat(macroCommandParameterWrapper.getNestedContexts().getLast()).isEqualTo(studentContext);
        verify(macroCommand).prepareStudentContext();
    }

    @Test
    void shouldCreateMacroContext_WithEmptyNestedContexts() {
        int parameter = 101;
        Input<Integer> inputParameter = Input.of(parameter);
        doCallRealMethod().when(doubleCommand).acceptPreparedContext(command, inputParameter);
        doCallRealMethod().when(booleanCommand).acceptPreparedContext(command, inputParameter);
        doCallRealMethod().when(intCommand).acceptPreparedContext(command, inputParameter);

        Context<Double> macroContext = command.createContext(inputParameter);

        assertThat(macroContext.isReady()).isTrue();
        command.fromNest().forEach(nestedCommand -> {
            verify(nestedCommand).acceptPreparedContext(command, inputParameter);
            if (nestedCommand instanceof RootCommand<?> rootCommand) {
                verify(rootCommand).createContext(inputParameter);
            } else {
                fail("NestedCommand not a RootCommand.");
            }
        });
        verify(command).prepareContext(doubleCommand, inputParameter);
        verify(command).prepareContext(booleanCommand, inputParameter);
        verify(command).prepareContext(intCommand, inputParameter);
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        wrapper.getNestedContexts().forEach(context -> assertThat(context).isNull());
    }

    @Test
    void shouldNotCreateMacroContext_MacroContextExceptionThrown() {
        int parameter = 102;
        Input<Integer> inputParameter = Input.of(parameter);
        doThrow(new CannotCreateCommandContextException(command.getId())).when(command).createContext(inputParameter);

        var exception = assertThrows(CannotCreateCommandContextException.class, () -> command.createContext(inputParameter));

        assertThat(exception).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(exception.getMessage()).isEqualTo("Cannot create command context for id: 'fake-command'");
        command.fromNest().forEach(nestedCommand ->
                verify(nestedCommand, never()).acceptPreparedContext(any(PrepareContextVisitor.class), any())
        );
    }

    @Test
    void shouldNotCreateMacroContext_DoubleContextExceptionThrown() {
        int parameter = 103;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContext(intCommand, inputParameter);
        allowRealPrepareContext(booleanCommand, inputParameter);
        allowRealAcceptPrepareContext(doubleCommand, inputParameter);
        doCallRealMethod().when(doubleCommand).createContextInit();
        doCallRealMethod().when(doubleCommand).createContext();
        doThrow(new CannotCreateCommandContextException("double")).when(doubleCommand).createContext(inputParameter);

        Context<Double> macroContext = command.createContext(inputParameter);

        assertThat(macroContext).isNotNull();
        assertThat(macroContext.isFailed()).isTrue();
        assertThat(macroContext.getException()).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(macroContext.getException().getMessage()).isEqualTo("Cannot create command context for id: 'double'");
        // check doubleCommand
        verify(doubleCommand).acceptPreparedContext(command, inputParameter);
        verify(command).prepareContext(doubleCommand, inputParameter);
        verify(doubleCommand).createContext(inputParameter);
        // check other commands
        verify(booleanCommand).acceptPreparedContext(eq(command), any());
        verify(intCommand).acceptPreparedContext(eq(command), any());
    }

    @Test
    void shouldNotCreateMacroContext_IntContextExceptionThrown() {
        int parameter = 104;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContext(doubleCommand, inputParameter);
        allowRealPrepareContext(booleanCommand, inputParameter);
        allowRealAcceptPrepareContext(intCommand, inputParameter);
        doCallRealMethod().when(intCommand).createContextInit();
        doCallRealMethod().when(intCommand).createContext();
        doThrow(new CannotCreateCommandContextException("int")).when(intCommand).createContext(inputParameter);

        Context<Double> macroContext = command.createContext(inputParameter);

        assertThat(macroContext).isNotNull();
        assertThat(macroContext.isFailed()).isTrue();
        assertThat(macroContext.getException()).isInstanceOf(CannotCreateCommandContextException.class);
        assertThat(macroContext.getException().getMessage()).isEqualTo("Cannot create command context for id: 'int'");
        command.fromNest().forEach(nestedCommand -> {
            verify(nestedCommand).acceptPreparedContext(command, inputParameter);
            if (nestedCommand instanceof RootCommand<?> rootCommand) {
                verify(rootCommand).createContext(inputParameter);
            } else {
                fail("NestedCommand not a RootCommand.");
            }
        });
        verify(command).prepareContext(doubleCommand, inputParameter);
        verify(command).prepareContext(booleanCommand, inputParameter);
        verify(command).prepareContext(intCommand, inputParameter);
    }

    @Test
    <N> void shouldDoMacroCommandRedo_Base() {
        int parameter = 105;
        Input<Integer> inputParameter = Input.of(parameter);
        Object[] parameters = new Object[]{parameter * 100, true, parameter * 10.0};
        RootCommand<?>[] nested = command.fromNest()
                .stream().map(RootCommand.class::cast)
                .toArray(RootCommand<?>[]::new);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        allowRealNestedCommandExecutionBase();
        IntStream.range(0, nested.length).forEach(i -> configureNestedRedoResult(nested[i], parameters[i]));

        command.doCommand(macroContext);

        assertThat(macroContext.isDone()).isTrue();
        verify(command).executeDo(macroContext);
        verify(command).doNestedCommands(eq(wrapper.getNestedContexts()), any(Context.StateChangedListener.class));
        wrapper.getNestedContexts().forEach(context -> {
            assertThat(context.isDone()).isTrue();
            Context<N> nestedContext = (Context<N>) context;
            RootCommand<N> nestedCommand = nestedContext.getCommand();
            verify(nestedCommand).doAsNestedCommand(eq(command), eq(context), any(Context.StateChangedListener.class));
            verify(command).doNestedCommand(eq(nestedCommand), eq(nestedContext), any(Context.StateChangedListener.class));
            verify(nestedCommand).doCommand(nestedContext);
        });
        IntStream.range(0, nested.length).forEach(i -> assertCommandResult(nested[i], wrapper, parameters[i]));
        Optional<?> lastContextResult = wrapper.getNestedContexts().getLast().getResult();
        assertThat(macroContext.getResult().orElseThrow()).isEqualTo(lastContextResult.orElseThrow());
    }

    @Test
    <N> void shouldDoMacroCommandRedo_PlusStudentCommand() {
        int parameter = 125;
        Input<Integer> inputParameter = Input.of(parameter);
        command = spy(new FakeMacroCommand(studentCommand));
        allowRealPrepareContextBase(inputParameter);
        allowRealPrepareContextExtra(inputParameter);
        command.putToNest(studentCommand);
        command.putToNest(doubleCommand);
        command.putToNest(booleanCommand);
        command.putToNest(intCommand);
        Object[] parameters = new Object[]{parameter * 100, true, parameter * 10.0};

        RootCommand<?>[] nested = command.fromNest().stream()
                .filter(nestedCommand -> nestedCommand != studentCommand)
                .map(RootCommand.class::cast)
                .toArray(RootCommand<?>[]::new);
        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
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
                    Context<N> nestedContext = (Context<N>) context;
                    RootCommand<N> nestedCommand = nestedContext.getCommand();
                    verify(nestedCommand).doAsNestedCommand(eq(command), eq(context), any(Context.StateChangedListener.class));
                    verify(nestedCommand).doCommand(nestedContext);
                });
        IntStream.range(0, nested.length).forEach(i -> assertCommandResult(nested[i], wrapper, parameters[i]));
        Optional<?> lastContextResult = wrapper.getNestedContexts().getLast().getResult();
        assertThat(macroContext.getResult().orElseThrow()).isEqualTo(lastContextResult.orElseThrow());
        verify(studentCommand, never()).doCommand(any(Context.class));
        assertCommandResult(studentCommand, wrapper, overridedStudentContext.getResult().orElseThrow());
        assertThat(wrapper.getNestedContexts().pop()).isEqualTo(overridedStudentContext);
    }

    @Test
    <N> void shouldDoMacroCommandRedo_PlusStudentAndMacroCommand() {
        int parameter = 126;
        Input<Integer> inputParameter = Input.of(parameter);
        // prepare nested macro-command
        var command1 = mock(RootCommand.class);
        var command2 = mock(CourseCommand.class);
        var command3 = mock(StudentCommand.class);
        final Double courseResult = 100.0;
        var macroCommand = spy(new MacroCommand<Double>() {
            @Override
            public String getId() {
                return "second-macro-command";
            }

            @Override
            public Logger getLog() {
                return LoggerFactory.getLogger("Second MacroCommand Logger");
            }

            @Override
            public <T> void doNestedCommand(final CourseCommand<T> command,
                                            final Context<T> doContext, final Context.StateChangedListener stateListener) {
                doCourseCommand(doContext, stateListener);
            }

            void doCourseCommand(Context<?> context, Context.StateChangedListener listener) {
                context.addStateListener(listener);
                context.setState(WORK);
                ((Context<Double>) context).setResult(courseResult);
                context.removeStateListener(listener);
            }
        });
        macroCommand.putToNest(command1);
        macroCommand.putToNest(command2);
        macroCommand.putToNest(command3);
        macroCommand.fromNest().forEach(mNested -> allowRealPrepareContext(macroCommand, mNested, inputParameter));
        // make root macro command
        command = spy(new FakeMacroCommand(studentCommand));
        command.putToNest(studentCommand);
        command.putToNest(doubleCommand);
        command.putToNest(booleanCommand);
        command.putToNest(intCommand);
        command.putToNest(macroCommand);

        Object[] parameters = new Object[]{parameter * 100, true, parameter * 10.0};
        Collection<NestedCommand<?>> commands = command.fromNest();
        RootCommand<?>[] nested = commands.stream()
                .filter(nestedCommand -> nestedCommand != studentCommand)
                .filter(nestedCommand -> nestedCommand != macroCommand)
                .map(RootCommand.class::cast)
                .toArray(RootCommand<?>[]::new);
        // allow real methods for mocks
        allowRealPrepareContextBase(command, inputParameter);
        allowRealPrepareContextExtra(command, inputParameter);
        // macro-command isn't mock but spy
        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext.isReady()).isTrue();
        // configure execution of second macro-command nested commands
        macroCommand.fromNest().forEach(mNested -> allowRealNestedCommandExecution(macroCommand, mNested));
        configureNestedRedoResult(command1, parameter * 200.);
        // command2 managed by macroCommand.doNestedCommand(...)
        configureNestedRedoResult(command3, parameter * 20.0);
        // configure execution of main macro-command
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        IntStream.range(0, nested.length).forEach(i -> configureNestedRedoResult(nested[i], parameters[i]));
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
                    Context<N> nestedContext = (Context<N>) context;
                    RootCommand<N> cmd = nestedContext.getCommand();
                    verify(cmd).doAsNestedCommand(eq(command), eq(context), any(Context.StateChangedListener.class));
                    verify(cmd).doCommand(nestedContext);
                });
        assertThat(macroContext.getResult().orElseThrow())
                .isEqualTo(wrapper.getNestedContexts().getLast().getResult().orElseThrow());
        // student-command behavior when did doAsNestedCommand(...) method
        verify(studentCommand, never()).doCommand(any(Context.class));
        assertCommandResult(studentCommand, wrapper, overridedStudentContext.getResult().orElseThrow());
        assertThat(wrapper.getNestedContexts().pop()).isEqualTo(overridedStudentContext);
        // check nested macro-command execution results
        Context<?> macroCommandContext = wrapper.getNestedContexts().getLast();
        MacroCommandParameter nestedWrapper = macroCommandContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(macroCommandContext.getCommand()).isEqualTo(macroCommand);
        assertThat(nestedWrapper.getRootInput().value()).isSameAs(parameter);
        nestedWrapper.getNestedContexts().forEach(context -> {
            assertThat(context.isDone()).isTrue();
            Context<N> nestedContext = (Context<N>) context;
            RootCommand<N> nestedCommand = nestedContext.getCommand();
            verify(nestedCommand).doAsNestedCommand(eq(macroCommand), eq(context), any(Context.StateChangedListener.class));
            if (nestedCommand == command2) {
                // course-command
                verify(macroCommand).doCourseCommand(eq(context), any(Context.StateChangedListener.class));
                verify(nestedCommand, never()).doCommand(nestedContext);
            } else {
                verify(nestedCommand).doCommand(nestedContext);
            }
        });
        // check interaction between nested macro command and nested commands during Do of nested-macro-command
        verify(macroCommand).doNestedCommands(eq(nestedWrapper.getNestedContexts()), any(Context.StateChangedListener.class));
        verify(macroCommand).doNestedCommand(eq(command1), any(Context.class), any(Context.StateChangedListener.class));
        verify(macroCommand).doNestedCommand(eq(command2), any(Context.class), any(Context.StateChangedListener.class));
        verify(macroCommand).doNestedCommand(eq(command3), any(Context.class), any(Context.StateChangedListener.class));
        Optional<?> lastContextResult = wrapper.getNestedContexts().getLast().getResult();
        assertThat(macroCommandContext.getResult().orElseThrow()).isEqualTo(lastContextResult.orElseThrow());
        assertThat(macroContext.getResult().orElseThrow()).isEqualTo(macroCommandContext.getResult().orElseThrow());
    }

    @Test
    void shouldDontMacroCommandRedo_NoNestedContextsChain() {
        int parameter = 106;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        wrapper.getNestedContexts().clear();

        command.doCommand(macroContext);

        assertThat(macroContext.isFailed()).isTrue();
        assertThat(macroContext.getException()).isInstanceOf(NoSuchElementException.class);
        verify(command).executeDo(macroContext);
        verify(command).doNestedCommands(eq(wrapper.getNestedContexts()), any(Context.StateChangedListener.class));
    }

    @Test
    void shouldDontMacroCommandRedo_NestedContextsAreNull() {
        int parameter = 107;
        Input<Integer> inputParameter = Input.of(parameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
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
        Input<Integer> inputParameter = Input.of(parameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        doThrow(UnableExecuteCommandException.class).when(command).executeDo(macroContext);

        Exception exception = assertThrows(UnableExecuteCommandException.class, () -> command.doCommand(macroContext));

        assertThat(exception).isInstanceOf(UnableExecuteCommandException.class);
        verify(command).executeDo(macroContext);
    }

    @Test
    <N> void shouldDontMacroCommandRedo_LastNestedContextThrows() {
        int parameter = 108;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        allowRealNestedCommandExecutionBase();
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        allowRealNestedCommandRollback(doubleCommand);
        allowRealNestedCommandRollback(booleanCommand);
        doThrow(UnableExecuteCommandException.class).when(intCommand).doCommand(any(Context.class));

        command.doCommand(macroContext);

        assertThat(macroContext.isFailed()).isTrue();
        verify(command).executeDo(macroContext);
        Context<N> doubleContext = (Context<N>) wrapper.getNestedContexts().getFirst();
        assertThat(doubleContext.isDone()).isTrue();
        assertThat(doubleContext.getResult().orElseThrow()).isEqualTo(parameter * 100.0);
        Context<N> intContext = (Context<N>) wrapper.getNestedContexts().getLast();
        assertThat(intContext.isFailed()).isTrue();
        assertThat(intContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        assertThat(macroContext.getException()).isEqualTo(intContext.getException());
        verifyNestedCommandDoExecution((RootCommand<N>) doubleCommand, doubleContext);
        verifyNestedCommandDoExecution((RootCommand<N>) intCommand, intContext);
        verify(command).undoNestedCommands(any(Input.class));
        verify(doubleCommand).undoCommand(wrapper.getNestedContexts().pop());
        verify(booleanCommand).undoCommand(wrapper.getNestedContexts().pop());
    }

    @Test
    <N> void shouldDontMacroCommandRedo_FirstNestedContextThrows() {
        int parameter = 109;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        allowRealNestedCommandExecutionBase();
        doThrow(UnableExecuteCommandException.class).when(doubleCommand).doCommand(any(Context.class));
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        allowRealNestedCommandRollback(intCommand);
        allowRealNestedCommandRollback(booleanCommand);

        command.doCommand(macroContext);

        verify(command).executeDo(macroContext);
        assertThat(macroContext.isFailed()).isTrue();
        Context<N> doubleContext = (Context<N>) wrapper.getNestedContexts().getFirst();
        assertThat(doubleContext.isFailed()).isTrue();
        assertThat(doubleContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        assertThat(macroContext.getException()).isEqualTo(doubleContext.getException());
        Context<N> intContext = (Context<N>) wrapper.getNestedContexts().getLast();
        assertThat(intContext.isDone()).isTrue();
        assertThat(intContext.getResult().orElseThrow()).isEqualTo(parameter * 10);
        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));
        verifyNestedCommandDoExecution((RootCommand<N>) doubleCommand, doubleContext);
        verifyNestedCommandDoExecution((RootCommand<N>) intCommand, intContext);
        verify(command).undoNestedCommands(any(Input.class));
        wrapper.getNestedContexts().pop();
        verify(booleanCommand).undoCommand(wrapper.getNestedContexts().pop());
        verify(intCommand).undoCommand(wrapper.getNestedContexts().pop());
    }

    @Test
    void shouldDoMacroCommandUndo_BaseCommands() {
        int parameter = 110;
        Input<Integer> inputParameter = Input.of(parameter);
        Object[] parameters = new Object[]{parameter * 100.0, true, parameter * 10.0};
        RootCommand<?>[] nested = command.fromNest()
                .stream().map(RootCommand.class::cast)
                .toArray(RootCommand<?>[]::new);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        IntStream.range(0, nested.length).forEach(i -> configureNestedRedoResult(nested[i], parameters[i]));
        allowRealNestedCommandExecutionBase();
        command.doCommand(macroContext);
        assertThat(macroContext.isDone()).isTrue();
        Deque<Context<?>> nestedDoneContexts = macroContext.<Deque<Context<?>>>getUndoParameter().value();
        assertThat(wrapper.getNestedContexts()).hasSameSizeAs(nestedDoneContexts);
        nestedDoneContexts.forEach(context -> assertThat(context.isDone()).isTrue());
        command.fromNest().forEach(this::configureNestedUndoStatus);
        allowRealNestedCommandRollbackBase();

        command.undoCommand(macroContext);

        assertThat(macroContext.getState()).isEqualTo(UNDONE);
        nestedDoneContexts.forEach(context -> assertThat(context.getState()).isEqualTo(UNDONE));

        verify(command).executeUndo(macroContext);
        verify(command).undoNestedCommands(any(Input.class));
        nestedDoneContexts.forEach(ctx -> verify(ctx.getCommand()).undoCommand(ctx));
    }

    @Test
    void shouldDoMacroCommandUndo_PlusStudentCommand() {
        int parameter = 120;
        Input<Integer> inputParameter = Input.of(parameter);
        Object[] parameters = new Object[]{parameter * 100.0, true, parameter * 10.0};
        command = spy(new FakeMacroCommand(studentCommand));
        command.putToNest(studentCommand);
        command.putToNest(doubleCommand);
        command.putToNest(booleanCommand);
        command.putToNest(intCommand);
        allowRealPrepareContextBase(inputParameter);
        allowRealPrepareContextExtra(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        RootCommand<?>[] nested = command.fromNest().stream()
                .filter(nestedCommand -> nestedCommand != studentCommand)
                .map(RootCommand.class::cast)
                .toArray(RootCommand<?>[]::new);
        IntStream.range(0, nested.length).forEach(i -> configureNestedRedoResult(nested[i], parameters[i]));
        allowRealNestedCommandExecutionBase();
        allowRealNestedCommandExecutionExtra();
        command.doCommand(macroContext);
        assertThat(macroContext.isDone()).isTrue();
        Deque<Context<?>> nestedDoneContexts = macroContext.<Deque<Context<?>>>getUndoParameter().value();
        assertThat(wrapper.getNestedContexts()).hasSameSizeAs(nestedDoneContexts);
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.isDone()).isTrue());
        List.of(nested).forEach(this::configureNestedUndoStatus);
        allowRealNestedCommandRollbackBase();
        allowRealNestedCommandRollbackExtra();

        command.undoCommand(macroContext);

        assertThat(macroContext.getState()).isEqualTo(UNDONE);
        nestedDoneContexts.stream()
                .filter(context -> context.getCommand() != studentCommand)
                .forEach(context -> assertThat(context.getState()).isEqualTo(UNDONE));

        verify(command).executeUndo(macroContext);
        verify(command).undoNestedCommands(any(Input.class));
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
    void shouldDoMacroCommandUndo_PlusStudentAndMacroCommand() {
        int parameter = 121;
        Input<Integer> inputParameter = Input.of(parameter);
        // prepare nested macro-command
        var command1 = mock(RootCommand.class);
        var command2 = mock(CourseCommand.class);
        var command3 = mock(StudentCommand.class);
        var macroCommand = spy(new MacroCommand<Double>() {
            @Override
            public String getId() {
                return "second-macro-command";
            }

            @Override
            public Logger getLog() {
                return LoggerFactory.getLogger("Second MacroCommand Logger");
            }

            @Override
            public Context<?> undoNestedCommand(CourseCommand<?> command, Context<?> undoContext) {
                return undoCourseCommand(undoContext);
            }

            Context<?> undoCourseCommand(Context<?> context) {
                context.setState(INIT);
                return context;
            }
        });
        macroCommand.putToNest(command1);
        macroCommand.putToNest(command2);
        macroCommand.putToNest(command3);
        {
            Object[] parameters = new Object[]{parameter * 200.0, true, parameter * 20.0};
            RootCommand<?>[] nested = macroCommand.fromNest()
                    .stream().map(RootCommand.class::cast)
                    .toArray(RootCommand<?>[]::new);
            IntStream.range(0, nested.length).forEach(i -> {
                allowRealPrepareContext(macroCommand, nested[i], inputParameter);
                configureNestedRedoResult(nested[i], parameters[i]);
                allowRealNestedCommandExecution(macroCommand, nested[i]);
                allowRealNestedCommandRollback(macroCommand, nested[i]);
            });
        }
        configureNestedUndoStatus(command1);
        configureNestedUndoStatus(command3);
        // make root macro command
        Object[] parameters = new Object[]{parameter * 100.0, true, parameter * 10.0};
        command = spy(new FakeMacroCommand(studentCommand));
        command.putToNest(studentCommand);
        command.putToNest(doubleCommand);
        command.putToNest(booleanCommand);
        command.putToNest(intCommand);
        command.putToNest(macroCommand);
        allowRealPrepareContextBase(inputParameter);
        allowRealPrepareContextExtra(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        RootCommand<?>[] nested = command.fromNest().stream()
                .filter(cmd -> cmd != studentCommand)
                .filter(cmd -> cmd != macroCommand)
                .map(RootCommand.class::cast)
                .toArray(RootCommand[]::new);
        IntStream.range(0, nested.length).forEach(i -> configureNestedRedoResult(nested[i], parameters[i]));
        allowRealNestedCommandExecutionBase();
        allowRealNestedCommandExecutionExtra();
        // do macro-command
        command.doCommand(macroContext);
        assertThat(macroContext.isDone()).isTrue();
        Deque<Context<?>> nestedDoneContexts = macroContext.<Deque>getUndoParameter().value();
        assertThat(wrapper.getNestedContexts()).hasSameSizeAs(nestedDoneContexts);
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.isDone()).isTrue());
        List.of(nested).forEach(this::configureNestedUndoStatus);
        allowRealNestedCommandRollbackBase();
        allowRealNestedCommandRollbackExtra();

        command.undoCommand(macroContext);

        // check main macro-command state
        assertThat(macroContext.getState()).isEqualTo(UNDONE);
        nestedDoneContexts.stream()
                .filter(context -> context.getCommand() != studentCommand)
                .forEach(context -> assertThat(context.getState()).isEqualTo(UNDONE));
        verify(command).executeUndo(macroContext);
        verify(command).undoNestedCommands(any(Input.class));
        nestedDoneContexts.stream()
                .filter(context -> context.getCommand() != studentCommand)
                .forEach(context -> {
                    final var nestedCommand = context.getCommand();
                    verify(nestedCommand).undoAsNestedCommand(command, context);
                    verify(nestedCommand).undoCommand(context);
                });

        // check canceled context calling
        verify(studentCommand, never()).undoCommand(any(Context.class));
        assertThat(nestedDoneContexts.pop().getState()).isEqualTo(CANCEL);

        // check successful contexts calling
        Deque<Context<?>> successDoneContexts = new LinkedList<>(nestedDoneContexts);
        verify(command).undoNestedCommand(doubleCommand, successDoneContexts.pop());
        verify(command).undoNestedCommand(booleanCommand, successDoneContexts.pop());
        verify(command).undoNestedCommand(intCommand, successDoneContexts.pop());
        verify(command).undoNestedCommand(macroCommand, successDoneContexts.pop());

        // check nested macro-command state
        Context<?> macroNestedContext = nestedDoneContexts.getLast();
        assertThat(macroNestedContext.getCommand()).isEqualTo(macroCommand);
        assertThat(macroNestedContext.getState()).isEqualTo(UNDONE);
        macroNestedContext.<Deque<Context<?>>>getUndoParameter().value().forEach(context -> {
            RootCommand<?> nestedCommand = context.getCommand();
            verify(nestedCommand).undoAsNestedCommand(macroCommand, context);
            if (nestedCommand != command2) {
                verify(nestedCommand).undoCommand(context);
                assertThat(context.getState()).isEqualTo(UNDONE);
            } else {
                verify(nestedCommand, never()).undoCommand(any(Context.class));
                verify(macroCommand).undoCourseCommand(context);
                assertThat(context.getState()).isEqualTo(INIT);
            }
        });
    }

    @Test
    void shouldDontMacroCommandUndo_MacroContextThrown() {
        int parameter = 111;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10.0);
        allowRealNestedCommandExecutionBase();
        command.doCommand(macroContext);
        assertThat(macroContext.isDone()).isTrue();
        Deque<Context<?>> nestedDoneContexts = macroContext.<Deque>getUndoParameter().value();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.isDone()).isTrue());
        doThrow(UnableExecuteCommandException.class).when(command).executeUndo(macroContext);

        Exception exception = assertThrows(UnableExecuteCommandException.class, () -> command.undoCommand(macroContext));

        assertThat(exception).isInstanceOf(UnableExecuteCommandException.class);
        verify(command).executeUndo(macroContext);
        verify(command, never()).undoNestedCommands(any(Input.class));
    }

    @Test
    <N> void shouldDontMacroCommandUndo_NestedContextThrown() {
        int parameter = 112;
        Input<Integer> inputParameter = Input.of(parameter);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10.0);
        allowRealNestedCommandExecutionBase();
        command.doCommand(macroContext);
        assertThat(macroContext.isDone()).isTrue();
        Deque<Context<?>> nestedDoneContexts = macroContext.<Deque>getUndoParameter().value();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.isDone()).isTrue());
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(intCommand);
        UnableExecuteCommandException exception = new UnableExecuteCommandException("boolean-command");
        doThrow(exception).when(booleanCommand).undoCommand(any(Context.class));
        allowRealNestedCommandRollbackBase();

        command.undoCommand(macroContext);

        verify(command).executeUndo(macroContext);
        verify(command).undoNestedCommands(any(Input.class));
        Context<N> current = (Context<N>) nestedDoneContexts.pop();
        assertThat(current.getState()).isEqualTo(DONE);
        verify(current.getCommand(), times(2)).doCommand(current);
        current = (Context<N>) nestedDoneContexts.pop();
        assertThat(current.isFailed()).isTrue();
        assertThat(macroContext.isFailed()).isTrue();
        assertThat(macroContext.getException()).isEqualTo(current.getException());
        current = (Context<N>) nestedDoneContexts.pop();
        verify(current.getCommand(), times(2)).doCommand(current);
        assertThat(current.getState()).isEqualTo(DONE);
    }

    @Test
    void shouldPrepareMacroContext() {
        int parameter = 113;
        Input<Integer> inputParameter = Input.of(parameter);
        doCallRealMethod().when(doubleCommand).createContext(inputParameter);

        Context<Double> doubleContext = (Context<Double>) command.prepareContext(doubleCommand, inputParameter);

        assertThat(doubleContext).isNotNull();
        assertThat(doubleContext.getCommand()).isEqualTo(doubleCommand);
        assertThat(doubleContext.getRedoParameter().value()).isSameAs(parameter);
        assertThat(doubleContext.getState()).isEqualTo(READY);

        verify(doubleCommand).createContext(inputParameter);
    }

    @Test
    void shouldNotPrepareMacroContext_ExceptionThrown() {
        int parameter = 114;
        Input<Integer> inputParameter = Input.of(parameter);
        when(doubleCommand.createContext(inputParameter)).thenThrow(new RuntimeException("cannot"));

        Exception exception = assertThrows(RuntimeException.class, () -> command.prepareContext(doubleCommand, inputParameter));

        assertThat(exception).isInstanceOf(RuntimeException.class);
        assertThat(exception.getMessage()).isEqualTo("cannot");
        verify(doubleCommand).createContext(inputParameter);
    }

    @Test
    void shouldDoNestedContexts() {
        int parameter = 115;
        Input<Integer> inputParameter = Input.of(parameter);
        AtomicInteger counter = new AtomicInteger(0);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        allowRealNestedCommandExecutionBase();
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        Context.StateChangedListener listener = (context, previous, newOne) -> {
            if (newOne == DONE) counter.incrementAndGet();
        };

        command.doNestedCommands(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isEqualTo(3);
        wrapper.getNestedContexts().forEach(context -> verifyDoneNestedContext(context, listener));
    }

    @Test
    void shouldNotDoNestedContexts_NestedRedoThrows() {
        int parameter = 116;
        Input<Integer> inputParameter = Input.of(parameter);
        AtomicInteger counter = new AtomicInteger(0);
        allowRealPrepareContextBase(inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        allowRealNestedCommandExecutionBase();
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        doThrow(UnableExecuteCommandException.class).when(intCommand).doCommand(any(Context.class));
        Context.StateChangedListener listener = (context, previous, newOne) -> {
            if (newOne == DONE) counter.incrementAndGet();
        };

        command.doNestedCommands(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isEqualTo(2);
        verifyDoneNestedContext(wrapper.getNestedContexts().pop(), listener);
        verifyDoneNestedContext(wrapper.getNestedContexts().pop(), listener);
        verifyFailedNestedContext(wrapper.getNestedContexts().pop(), listener, UnableExecuteCommandException.class);
    }

    @Test
    <T> void shouldDoAsNestedCommand() {
        int parameter = 117;
        Input<Integer> inputParameter = Input.of(parameter);
        AtomicInteger counter = new AtomicInteger(0);
        allowRealPrepareContext(doubleCommand, inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        allowRealNestedCommandExecution(doubleCommand);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        Context.StateChangedListener listener = (context, previous, newOne) -> {
            if (newOne == DONE) counter.incrementAndGet();
        };
        Context<T> done = (Context<T>) wrapper.getNestedContexts().getFirst();

        doubleCommand.doAsNestedCommand(command, done, listener);

        assertThat(counter.get()).isEqualTo(1);
        verify(done.getCommand()).doCommand(done);
        assertThat(done.getState()).isEqualTo(DONE);

    }

    @Test
    <T> void shouldNotDoAsNestedCommand_EmptyContext() {
        int parameter = 118;
        Input<Integer> inputParameter = Input.of(parameter);
        AtomicInteger counter = new AtomicInteger(0);
        Context<Double> macroContext = command.createContext(inputParameter);
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        assertThat(wrapper.getNestedContexts().getFirst()).isNull();
        Context.StateChangedListener listener = (context, previous, newOne) -> {
            if (newOne == DONE) counter.incrementAndGet();
        };

        assertThrows(NullPointerException.class, () -> command.doNestedCommand((RootCommand<T>) null, null, listener));

        assertThat(counter.get()).isZero();
    }

    @Test
    <T> void shouldNotDoAsNestedCommand_NestedContextRedoThrows() {
        int parameter = 119;
        Input<Integer> inputParameter = Input.of(parameter);
        AtomicInteger counter = new AtomicInteger(0);
        allowRealPrepareContext(doubleCommand, inputParameter);
        Context<Double> macroContext = command.createContext(inputParameter);
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput().value()).isSameAs(parameter);
        allowRealNestedCommandExecution(doubleCommand);
        doThrow(UnableExecuteCommandException.class).when(doubleCommand).doCommand(any(Context.class));
        Context.StateChangedListener listener = (context, previous, newOne) -> {
            if (newOne == DONE) counter.incrementAndGet();
        };
        Context<T> done = (Context<T>) wrapper.getNestedContexts().getFirst();

        doubleCommand.doAsNestedCommand(command, done, listener);

        assertThat(counter.get()).isZero();
        assertThat(done.isFailed()).isTrue();
        assertThat(done.getException()).isInstanceOf(UnableExecuteCommandException.class);
        verify(done.getCommand()).doCommand(done);
    }

    // inner classes
    static class FakeMacroCommand extends MacroCommand<Double> {
        static CommandContext<?> overridedStudentContext;
        Logger logger = LoggerFactory.getLogger(FakeMacroCommand.class);

        public FakeMacroCommand(StudentCommand<Double> student) {
            overridedStudentContext = CommandContext.<Double>builder().command(student).state(INIT).build();
        }

        @Override
        public <N> Context<N> prepareContext(StudentCommand<N> command, Input<?> mainInput) {
            return prepareStudentContext();
        }

        <N> Context<N> prepareStudentContext() {
            overridedStudentContext.setRedoParameter(Input.of(200));
            return (Context<N>) overridedStudentContext;
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

        public String getId() {
            return "fake-command";
        }
    }

    // private methods
    private <N> void verifyDoneNestedContext(final Context<?> nestedContext, final Context.StateChangedListener listener) {
        Context<N> current = (Context<N>) nestedContext;
        RootCommand<N> nestedCommand = current.getCommand();
        verify(nestedCommand).doAsNestedCommand(command, current, listener);
        verify(command).doNestedCommand(nestedCommand, current, listener);
        verify(nestedCommand).doCommand(current);
        assertThat(current.isDone()).isTrue();
    }

    private <N> void verifyFailedNestedContext(final Context<?> nestedContext,
                                               final Context.StateChangedListener listener,
                                               final Class<? extends Exception> clazz) {
        Context<N> current = (Context<N>) nestedContext;
        RootCommand<N> nestedCommand = current.getCommand();
        verify(nestedCommand).doAsNestedCommand(command, current, listener);
        verify(command).doNestedCommand(nestedCommand, current, listener);
        verify(nestedCommand).doCommand(current);
        assertThat(current.isFailed()).isTrue();
        assertThat(current.getException()).isInstanceOf(clazz);
    }

    private MacroCommandParameter checkMainMacroCommandState(Context<Double> macroContext,
                                                             Input<?> parameter, MacroCommand<Void> macroCommand) {
        assertThat(macroContext.isReady()).isTrue();
        MacroCommandParameter wrapper = macroContext.<MacroCommandParameter>getRedoParameter().value();
        assertThat(wrapper.getRootInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().stream()
                .filter(context -> context.getCommand() != studentCommand)
                .filter(context -> context.getCommand() != macroCommand)
                .forEach(context -> {
                    assertThat(context.isReady()).isTrue();
                    RootCommand<?> nestedCommand = context.getCommand();
                    assertThat(context.<Object>getRedoParameter()).isEqualTo(parameter);
                    verify(nestedCommand).acceptPreparedContext(command, parameter);
                    verify(command).prepareContext(nestedCommand, parameter);
                    verify(nestedCommand).createContext(parameter);
                });
        // check studentCommand context
        assertThat(wrapper.getNestedContexts().getFirst()).isEqualTo(overridedStudentContext);
        assertThat(overridedStudentContext.getCommand()).isEqualTo(studentCommand);
        assertThat(overridedStudentContext.getRedoParameter().value()).isEqualTo(200);
        verify(studentCommand).acceptPreparedContext(command, parameter);
        verify(command).prepareContext(studentCommand, parameter);
        verify(studentCommand, never()).createContext(any());
        verify(command).prepareStudentContext();
        return wrapper;
    }

    private static void assertCommandResult(RootCommand<?> cmd, MacroCommandParameter wrapper, Object expected) {
        var result = wrapper.getNestedContexts().stream()
                .filter(context -> context.getCommand().equals(cmd))
                .map(context -> context.getResult().orElseThrow())
                .findFirst().orElseThrow();
        assertThat(result).isEqualTo(expected);
    }

    private <T> void verifyNestedCommandDoExecution(RootCommand<T> cmd, Context<T> context) {
        verify(cmd).doAsNestedCommand(eq(command), eq(context), any(Context.StateChangedListener.class));
        verify(command).doNestedCommand(eq(cmd), eq(context), any(Context.StateChangedListener.class));
        verify(cmd).doCommand(context);
    }

    private void checkNestedContext(Context<?> nestedContext, RootCommand<?> command, Object parameter) {
        assertThat(nestedContext).isNotNull();
        assertThat(nestedContext.isReady()).isTrue();
        assertThat(nestedContext.getCommand()).isEqualTo(command);
        assertThat(nestedContext.<Object>getRedoParameter()).isEqualTo(parameter);
    }

    private void allowRealPrepareContext(PrepareContextVisitor macro, NestedCommand<?> nested, Input<?> parameter) {
        allowRealPrepareContext(macro, (RootCommand<?>) nested, parameter);
    }

    private void allowRealPrepareContext(PrepareContextVisitor macro, RootCommand<?> nested, Input<?> parameter) {
        doCallRealMethod().when(nested).createContext(parameter);
        allowRealAcceptPrepareContext(macro, nested, parameter);
    }

    private void allowRealPrepareContext(RootCommand<?> nested, Input<?> parameter) {
        allowRealPrepareContext(command, nested, parameter);
    }

    private void allowRealAcceptPrepareContext(RootCommand<?> nested, Input<?> parameter) {
        allowRealAcceptPrepareContext(command, nested, parameter);
    }

    private void allowRealAcceptPrepareContext(PrepareContextVisitor macro, RootCommand<?> nested, Input<?> parameter) {
        doCallRealMethod().when(nested).acceptPreparedContext(macro, parameter);
    }

    private void allowRealPrepareContextBase(PrepareContextVisitor macro, Input<?> parameter) {
        allowRealPrepareContext(macro, doubleCommand, parameter);
        allowRealPrepareContext(macro, booleanCommand, parameter);
        allowRealPrepareContext(macro, intCommand, parameter);
    }

    private void allowRealPrepareContextBase(Input<?> parameter) {
        allowRealPrepareContextBase(command, parameter);
    }

    private void allowRealPrepareContextExtra(PrepareContextVisitor macro, Input<?> parameter) {
        allowRealAcceptPrepareContext(macro, studentCommand, parameter);
    }

    private void allowRealPrepareContextExtra(Input<?> parameter) {
        allowRealPrepareContextExtra(command, parameter);
    }

    private void allowRealNestedCommandExecution(NestedCommandExecutionVisitor macro, RootCommand<?> nested) {
        doCallRealMethod().when(nested).doAsNestedCommand(eq(macro), any(Context.class), any(Context.StateChangedListener.class));
    }

    private void allowRealNestedCommandExecution(NestedCommandExecutionVisitor macro, NestedCommand<?> nested) {
        doCallRealMethod().when(nested).doAsNestedCommand(eq(macro), any(Context.class), any(Context.StateChangedListener.class));
    }

    private void allowRealNestedCommandExecution(RootCommand<?> nested) {
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

    private void allowRealNestedCommandRollback(NestedCommandExecutionVisitor macro, RootCommand<?> nested) {
        doCallRealMethod().when(nested).undoAsNestedCommand(eq(macro), any(Context.class));
    }

    private void allowRealNestedCommandRollback(RootCommand<?> nested) {
        allowRealNestedCommandRollback(command, nested);
    }

    private void allowRealNestedCommandRollbackBase() {
        allowRealNestedCommandRollback(doubleCommand);
        allowRealNestedCommandRollback(booleanCommand);
        allowRealNestedCommandRollback(intCommand);
    }

    private void allowRealNestedCommandRollbackExtra() {
        allowRealNestedCommandRollback(studentCommand);
    }

    private <T> void configureNestedRedoResult(RootCommand<?> nestedCommand, Object result) {
        doAnswer(invocationOnMock -> {
            Context<T> context = invocationOnMock.getArgument(0, Context.class);
            context.setState(WORK);
            context.setResult((T) result);
            return null;
        }).when(nestedCommand).doCommand(any(Context.class));
    }

    private <T> void configureNestedUndoStatus(RootCommand<T> nextedCommand) {
        doAnswer(invocationOnMock -> {
            Context<T> context = invocationOnMock.getArgument(0, Context.class);
            context.setState(WORK);
            context.setState(UNDONE);
            return null;
        }).when(nextedCommand).undoCommand(any(Context.class));
    }

    private void configureNestedUndoStatus(NestedCommand<?> nextedCommand) {
        configureNestedUndoStatus((RootCommand<?>) nextedCommand);
    }
}