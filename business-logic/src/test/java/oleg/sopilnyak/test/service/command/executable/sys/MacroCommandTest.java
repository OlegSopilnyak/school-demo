package oleg.sopilnyak.test.service.command.executable.sys;

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
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MacroCommandTest {
    @Spy
    @InjectMocks
    FakeMacroCommand command;
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
        command.add(doubleCommand);
        command.add(booleanCommand);
        command.add(intCommand);
    }

    @Test
    void checkIntegrity() {
        assertThat(command).isNotNull();
    }

    @Test
    <T> void shouldCreateMacroContext() {
        int parameter = 100;
        allowCreateRealContexts(parameter);

        Context<Integer> macroContext = command.createContext(parameter);

        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        checkNestedContext(wrapper.getNestedContexts().pop(), doubleCommand, parameter);
        checkNestedContext(wrapper.getNestedContexts().pop(), booleanCommand, parameter);
        checkNestedContext(wrapper.getNestedContexts().pop(), intCommand, parameter);
        verify(command).prepareContext(doubleCommand, parameter);
        verify(command).prepareContext(booleanCommand, parameter);
        verify(command).prepareContext(intCommand, parameter);
    }

    @Test
    <T> void shouldCreateMacroContext_WithEmptyNestedContexts() {
        int parameter = 101;
        doCallRealMethod().when(doubleCommand).acceptPreparedContext(command, parameter);
        doCallRealMethod().when(booleanCommand).acceptPreparedContext(command, parameter);
        doCallRealMethod().when(intCommand).acceptPreparedContext(command, parameter);

        Context<Integer> macroContext = command.createContext(parameter);

        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx).isNull());

        verify(command).prepareContext(doubleCommand, parameter);
        verify(command).prepareContext(booleanCommand, parameter);
        verify(command).prepareContext(intCommand, parameter);
    }

    @Test
    void shouldNotCreateMacroContext_MacroContextExceptionThrown() {
        int parameter = 102;
        doCallRealMethod().when(doubleCommand).acceptPreparedContext(command, parameter);
        doCallRealMethod().when(booleanCommand).acceptPreparedContext(command, parameter);
        doCallRealMethod().when(intCommand).acceptPreparedContext(command, parameter);
        String commandId = command.getId();
        when(command.createContext(parameter)).thenThrow(new UnableExecuteCommandException(commandId));

        var ex = assertThrows(UnableExecuteCommandException.class, () -> command.createContext(parameter));

        assertThat(ex.getMessage()).isEqualTo("Cannot execute command 'fake-command'");
        verify(command, atLeastOnce()).prepareContext(doubleCommand, parameter);
        verify(command, atLeastOnce()).prepareContext(booleanCommand, parameter);
        verify(command, atLeastOnce()).prepareContext(intCommand, parameter);
    }

    @Test
    void shouldNotCreateMacroContext_DoubleContextExceptionThrown() {
        int parameter = 103;
        doCallRealMethod().when(doubleCommand).acceptPreparedContext(command, parameter);
        when(command.prepareContext(doubleCommand, parameter)).thenThrow(new UnableExecuteCommandException("double"));

        var ex = assertThrows(UnableExecuteCommandException.class, () -> command.createContext(parameter));
        assertThat(ex.getMessage()).isEqualTo("Cannot execute command 'double'");

        verify(command, atLeastOnce()).prepareContext(doubleCommand, parameter);
        verify(command, never()).prepareContext(booleanCommand, parameter);
        verify(command, never()).prepareContext(intCommand, parameter);
    }

    @Test
    void shouldNotCreateMacroContext_IntContextExceptionThrown() {
        int parameter = 104;
        allowCreateRealContexts(parameter);
        when(command.prepareContext(intCommand, parameter)).thenThrow(new UnableExecuteCommandException("int"));

        var ex = assertThrows(UnableExecuteCommandException.class, () -> command.createContext(parameter));
        assertThat(ex.getMessage()).isEqualTo("Cannot execute command 'int'");

        verify(command).prepareContext(doubleCommand, parameter);
        verify(command, atLeastOnce()).prepareContext(booleanCommand, parameter);
        verify(command, atLeastOnce()).prepareContext(intCommand, parameter);
    }


    @Test
    <T> void shouldCreateMacroContext_StudentCommand() {
        int parameter = 115;
        command = spy(new FakeMacroCommand());
        allowCreateRealContexts(parameter);
        doCallRealMethod().when(studentCommand).acceptPreparedContext(command, parameter);
        command.add(studentCommand);
        command.add(doubleCommand);
        command.add(booleanCommand);
        command.add(intCommand);

        Context<Integer> macroContext = command.createContext(parameter);

        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        assertThat(wrapper.getNestedContexts().getFirst()).isEqualTo(FakeMacroCommand.CONTEXT);
        assertThat(wrapper.getNestedContexts().getLast()).isNotNull();
        verify(command).prepareContext(studentCommand, parameter);
        verify(command).prepareContext(doubleCommand, parameter);
        verify(command).prepareContext(booleanCommand, parameter);
        verify(command).prepareContext(intCommand, parameter);
        verify(studentCommand).acceptPreparedContext(command, parameter);
        verify(studentCommand, never()).createContext(any());
        verify(doubleCommand).acceptPreparedContext(command, parameter);
        verify(doubleCommand).createContext(parameter);
        verify(booleanCommand).acceptPreparedContext(command, parameter);
        verify(booleanCommand).createContext(parameter);
        verify(intCommand).acceptPreparedContext(command, parameter);
        verify(intCommand).createContext(parameter);
    }

    @Test
    <T> void shouldDoMacroCommandRedo() {
        int parameter = 105;
        allowCreateRealContexts(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        Context<Integer> macroContext = command.createContext(parameter);

        command.doCommand(macroContext);

        verify(command).executeDo(macroContext);
        assertThat(macroContext.getState()).isEqualTo(DONE);
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(context -> assertThat(context.isDone()).isTrue());

        Context<T> firstNestedContext = wrapper.getNestedContexts().getFirst();
        assertThat(firstNestedContext.getResult().orElseThrow()).isEqualTo(parameter * 100.0);
        Context<T> lastNestedContext = wrapper.getNestedContexts().getLast();
        assertThat(lastNestedContext.getResult().orElseThrow()).isEqualTo(parameter * 10);
        assertThat(macroContext.getResult()).isEqualTo(lastNestedContext.getResult());

        verify(command).doNestedCommands(eq(wrapper.getNestedContexts()), any(Context.StateChangedListener.class));
        verify(command).doNestedCommand(eq(command), eq(firstNestedContext), any(Context.StateChangedListener.class));
        verify(doubleCommand).doCommand(firstNestedContext);
        verify(command).doNestedCommand(eq(command), eq(lastNestedContext), any(Context.StateChangedListener.class));
        verify(intCommand).doCommand(lastNestedContext);
    }

    @Test
    <T> void shouldDontMacroCommandRedo_NoNestedContextsChain() {
        int parameter = 106;
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        assertThat(macroContext.<Object>getRedoParameter()).isInstanceOf(MacroCommandParameter.class);
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);

        wrapper.getNestedContexts().clear();

        command.doCommand(macroContext);

        assertThat(macroContext.getState()).isEqualTo(FAIL);
        assertThat(macroContext.getException()).isInstanceOf(NoSuchElementException.class);
        verify(command).executeDo(macroContext);
        verify(command).doNestedCommands(eq(wrapper.getNestedContexts()), any(Context.StateChangedListener.class));
    }

    @Test
    <T> void shouldDontMacroCommandRedo_NestedContextsAreNull() {
        int parameter = 107;
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        assertThat(macroContext.<Object>getRedoParameter()).isInstanceOf(MacroCommandParameter.class);
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);

        command.doCommand(macroContext);

        assertThat(macroContext.getState()).isEqualTo(FAIL);
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
        allowCreateRealContexts(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        doThrow(UnableExecuteCommandException.class).when(intCommand).doCommand(any(Context.class));
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        assertThat(macroContext.<Object>getRedoParameter()).isInstanceOf(MacroCommandParameter.class);

        command.doCommand(macroContext);

        verify(command).executeDo(macroContext);
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        Context<Double> doubleContext = (Context<Double>) wrapper.getNestedContexts().getFirst();
        Context<Integer> intContext = (Context<Integer>) wrapper.getNestedContexts().getLast();
        assertThat(doubleContext.getState()).isEqualTo(DONE);
        assertThat(doubleContext.getResult().orElse(null)).isEqualTo(parameter * 100.0);
        assertThat(intContext.getState()).isEqualTo(FAIL);
        assertThat(intContext.getException()).isInstanceOf(UnableExecuteCommandException.class);

        verify(command).doNestedCommands(eq(wrapper.getNestedContexts()), any(Context.StateChangedListener.class));

        verify(command).doNestedCommand(eq(doubleCommand), eq(doubleContext), any(Context.StateChangedListener.class));
        verify(doubleCommand).doCommand(doubleContext);
        verify(command).doNestedCommand(eq(intCommand), eq(intContext), any(Context.StateChangedListener.class));
        verify(intCommand).doCommand(intContext);

        assertThat(macroContext.getState()).isEqualTo(FAIL);
        assertThat(macroContext.getException()).isEqualTo(intContext.getException());
        verify(command).rollbackDoneContexts(any(Deque.class));
        verify(doubleCommand).undoCommand(wrapper.getNestedContexts().pop());
        verify(booleanCommand).undoCommand(wrapper.getNestedContexts().pop());
    }

    @Test
    <T> void shouldDontMacroCommandRedo_FirstNestedContextThrows() {
        int parameter = 109;
        allowCreateRealContexts(parameter);
        doThrow(UnableExecuteCommandException.class).when(doubleCommand).doCommand(any(Context.class));
        configureNestedRedoResult(intCommand, parameter * 10);
        configureNestedRedoResult(booleanCommand, true);
        Context<Integer> macroContext = command.createContext(parameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        assertThat(macroContext.<Object>getRedoParameter()).isInstanceOf(MacroCommandParameter.class);

        command.doCommand(macroContext);

        verify(command).executeDo(macroContext);
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        Context<?> doubleContext = wrapper.getNestedContexts().getFirst();
        Context<?> intContext = wrapper.getNestedContexts().getLast();
        assertThat(doubleContext.getState()).isEqualTo(FAIL);
        assertThat(doubleContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        assertThat(intContext.getState()).isEqualTo(DONE);
        assertThat(intContext.getResult().orElse(null)).isEqualTo(parameter * 10);

        verify(command).doNestedCommands(any(Deque.class), any(Context.StateChangedListener.class));

        verify(command).doNestedCommand(eq(doubleCommand), eq(doubleContext), any(Context.StateChangedListener.class));
        verify(doubleCommand).doCommand(doubleContext);
        verify(command).doNestedCommand(eq(intCommand), eq(intContext), any(Context.StateChangedListener.class));
        verify(intCommand).doCommand(intContext);

        assertThat(macroContext.getState()).isEqualTo(FAIL);
        assertThat(macroContext.getException()).isEqualTo(doubleContext.getException());
        verify(command).rollbackDoneContexts(any(Deque.class));
        wrapper.getNestedContexts().pop();
        verify(booleanCommand).undoCommand(wrapper.getNestedContexts().pop());
        verify(intCommand).undoCommand(wrapper.getNestedContexts().pop());
    }

    @Test
    <T> void shouldDoMacroCommandUndo() {
        int parameter = 110;
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        command.doCommand(macroContext);
        assertThat(macroContext.getState()).isEqualTo(DONE);
        Deque<Context<T>> nestedDoneContexts = macroContext.getUndoParameter();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.getState()).isEqualTo(DONE));
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);

        command.undoCommand(macroContext);

        assertThat(macroContext.getState()).isEqualTo(UNDONE);
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.getState()).isEqualTo(UNDONE));

        verify(command).executeUndo(macroContext);
        verify(command).rollbackDoneContexts(nestedDoneContexts);
        nestedDoneContexts.forEach(ctx -> verify(ctx.getCommand()).undoCommand(ctx));
    }

    @Test
    <T> void shouldDontMacroCommandUndo_MacroContextThrown() {
        int parameter = 111;
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        command.doCommand(macroContext);
        assertThat(macroContext.getState()).isEqualTo(DONE);
        Deque<Context<T>> nestedDoneContexts = macroContext.getUndoParameter();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.getState()).isEqualTo(DONE));
        doThrow(UnableExecuteCommandException.class).when(command).executeUndo(macroContext);

        Exception exception = assertThrows(UnableExecuteCommandException.class, () -> command.undoCommand(macroContext));

        assertThat(exception).isInstanceOf(UnableExecuteCommandException.class);
        verify(command).executeUndo(macroContext);
        verify(command, never()).rollbackDoneContexts(nestedDoneContexts);
    }

    @Test
    <T> void shouldDontMacroCommandUndo_NestedContextThrown() {
        int parameter = 112;
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        command.doCommand(macroContext);
        assertThat(macroContext.getState()).isEqualTo(DONE);
        Deque<Context<T>> nestedDoneContexts = macroContext.getUndoParameter();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.getState()).isEqualTo(DONE));
        configureNestedUndoStatus(doubleCommand);
        doThrow(UnableExecuteCommandException.class).when(booleanCommand).undoCommand(any(Context.class));
        configureNestedUndoStatus(intCommand);

        command.undoCommand(macroContext);

        verify(command).executeUndo(macroContext);
        verify(command).rollbackDoneContexts(nestedDoneContexts);
        Context<?> current = nestedDoneContexts.pop();
        assertThat(current.getState()).isEqualTo(UNDONE);
        current = nestedDoneContexts.pop();
        assertThat(current.getState()).isEqualTo(FAIL);
        assertThat(macroContext.getState()).isEqualTo(FAIL);
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
    <T> void shouldRunNestedContexts() {
        int parameter = 115;
        AtomicInteger counter = new AtomicInteger(0);
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
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
        wrapper.getNestedContexts().forEach(ctx -> {
            verify(ctx.getCommand()).doCommand(ctx);
            assertThat(ctx.getState()).isEqualTo(DONE);
        });
    }

    @Test
    <T> void shouldNotRunNestedContexts_NestedRedoThrows() {
        int parameter = 116;
        AtomicInteger counter = new AtomicInteger(0);
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
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
        Context<?> current = wrapper.getNestedContexts().pop();
        verify(current.getCommand()).doCommand(current);
        assertThat(current.getState()).isEqualTo(DONE);
        current = wrapper.getNestedContexts().pop();
        verify(current.getCommand()).doCommand(current);
        assertThat(current.getState()).isEqualTo(DONE);
        current = wrapper.getNestedContexts().pop();
        verify(current.getCommand()).doCommand(current);
        assertThat(current.getState()).isEqualTo(FAIL);
        assertThat(current.getException()).isInstanceOf(UnableExecuteCommandException.class);
    }

    @Test
    <T> void shouldRunNestedCommand() {
        int parameter = 117;
        AtomicInteger counter = new AtomicInteger(0);
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        Context.StateChangedListener<T> listener = (context, previous, newOne) -> {
            if (newOne == DONE) {
                counter.incrementAndGet();
            }
        };

        Context<T> done = wrapper.getNestedContexts().getFirst();
//        command.doNestedCommand(done.getCommand(), done, listener);

        assertThat(counter.get()).isEqualTo(1);
        verify(done.getCommand()).doCommand(done);
        assertThat(done.getState()).isEqualTo(DONE);

    }

    @Test
    <T> void shouldNotRunNestedCommand_EmptyContext() {
        int parameter = 118;
        AtomicInteger counter = new AtomicInteger(0);
        Context<Integer> macroContext = command.createContext(parameter);
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        assertThat(wrapper.getNestedContexts().getFirst()).isNull();
        Context.StateChangedListener<T> listener = (context, previous, newOne) -> {
        };

        assertThrows(NullPointerException.class, () -> command.doNestedCommand((SchoolCommand) null, null, listener));

        assertThat(counter.get()).isZero();
    }

    @Test
    <T> void shouldNotRunNestedCommand_NestedContextRedoThrows() {
        int parameter = 119;
        AtomicInteger counter = new AtomicInteger(0);
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = command.createContext(parameter);
        MacroCommandParameter<T> wrapper = macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        doThrow(UnableExecuteCommandException.class).when(doubleCommand).doCommand(any(Context.class));
        Context.StateChangedListener<T> listener = (context, previous, newOne) -> {
            if (newOne == DONE) {
                counter.incrementAndGet();
            }
        };

        Context<T> done = wrapper.getNestedContexts().getFirst();
//        command.doNestedCommand(doubleCommand, done, listener);

        assertThat(counter.get()).isZero();
        assertThat(done.getState()).isEqualTo(FAIL);
        assertThat(done.getException()).isInstanceOf(UnableExecuteCommandException.class);
    }

    private static class FakeMacroCommand extends MacroCommand<SchoolCommand> {
        static Context<?> CONTEXT = CommandContext.builder().build();
        Logger logger = LoggerFactory.getLogger(FakeMacroCommand.class);

        /**
         * To prepare context for particular type of the nested command
         *
         * @param command   nested command instance
         * @param mainInput macro-command input parameter
         * @param <T>       type of command's do result
         * @return built context of the command for input parameter
         * @see StudentCommand
         * @see StudentCommand#createContext(Object)
         * @see Context
         */
        @Override
        public <T> Context<T> prepareContext(StudentCommand command, Object mainInput) {
            return (Context<T>) CONTEXT;
        }

        @Override
        public Logger getLog() {
            return logger;
        }

        public String getId() {
            return "fake-command";
        }
    }

    private <N> void checkNestedContext(Context<N> nestedContext, SchoolCommand command, Object parameter) {
        assertThat(nestedContext).isNotNull();
        assertThat(nestedContext.getState()).isEqualTo(READY);
        assertThat(nestedContext.getCommand()).isEqualTo(command);
        assertThat(nestedContext.<Object>getRedoParameter()).isEqualTo(parameter);
    }

    private void allowRealPrepareContext(SchoolCommand nested, Object parameter) {
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
    }

    private void allowRealNestedCommandExecution(SchoolCommand nested) {
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

    private void allowCreateRealContexts(Object parameter) {
        doCallRealMethod().when(doubleCommand).createContext(parameter);
        doCallRealMethod().when(booleanCommand).createContext(parameter);
        doCallRealMethod().when(intCommand).createContext(parameter);
        // visitor accept
        doCallRealMethod().when(doubleCommand).acceptPreparedContext(command, parameter);
        doCallRealMethod().when(booleanCommand).acceptPreparedContext(command, parameter);
        doCallRealMethod().when(intCommand).acceptPreparedContext(command, parameter);
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