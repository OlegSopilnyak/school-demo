package oleg.sopilnyak.test.service.command.executable.sys;

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
    void shouldCreateMacroContext() {
        int parameter = 100;
        allowCreateRealContexts(parameter);

        Context<Integer> macroContext = (Context<Integer>) command.createContext(parameter);

        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        assertThat(macroContext.getRedoParameter()).isInstanceOf(CommandParameterWrapper.class);
        CommandParameterWrapper wrapper = (CommandParameterWrapper) macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);

        Context<?> doubleContext = wrapper.getNestedContexts().pop();
        assertThat(doubleContext).isNotNull();
        assertThat(doubleContext.getState()).isEqualTo(READY);
        assertThat(doubleContext.getCommand()).isEqualTo(doubleCommand);
        assertThat(doubleContext.getRedoParameter()).isEqualTo(parameter);

        Context<?> booleanContext = wrapper.getNestedContexts().pop();
        assertThat(booleanContext).isNotNull();
        assertThat(booleanContext.getState()).isEqualTo(READY);
        assertThat(booleanContext.getCommand()).isEqualTo(booleanCommand);
        assertThat(booleanContext.getRedoParameter()).isEqualTo(parameter);

        Context<?> intContext = wrapper.getNestedContexts().pop();
        assertThat(intContext).isNotNull();
        assertThat(intContext.getState()).isEqualTo(READY);
        assertThat(intContext.getCommand()).isEqualTo(intCommand);
        assertThat(intContext.getRedoParameter()).isEqualTo(parameter);

        verify(command).prepareContext(doubleCommand, parameter);
        verify(command).prepareContext(booleanCommand, parameter);
        verify(command).prepareContext(intCommand, parameter);
    }

    @Test
    void shouldCreateMacroContext_WithEmptyNestedContexts() {
        int parameter = 101;

        Context<Integer> macroContext = (Context<Integer>) command.createContext(parameter);

        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        assertThat(macroContext.getRedoParameter()).isInstanceOf(CommandParameterWrapper.class);
        CommandParameterWrapper wrapper = (CommandParameterWrapper) macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx).isNull());

        verify(command).prepareContext(doubleCommand, parameter);
        verify(command).prepareContext(booleanCommand, parameter);
        verify(command).prepareContext(intCommand, parameter);
    }

    @Test
    void shouldNotCreateMacroContext_MacroContextExceptionThrown() {
        int parameter = 102;
        String commandId = command.getId();
        when(command.createContext(parameter)).thenThrow(new UnableExecuteCommandException(commandId));

        Exception ex = assertThrows(UnableExecuteCommandException.class, () -> command.createContext(parameter));
        assertThat(ex.getMessage()).isEqualTo("Cannot execute command 'fake-command'");

        verify(command, atLeastOnce()).prepareContext(doubleCommand, parameter);
        verify(command, atLeastOnce()).prepareContext(booleanCommand, parameter);
        verify(command, atLeastOnce()).prepareContext(intCommand, parameter);
    }

    @Test
    void shouldNotCreateMacroContext_DoubleContextExceptionThrown() {
        int parameter = 103;
        when(command.prepareContext(doubleCommand, parameter)).thenThrow(new UnableExecuteCommandException("double"));

        Exception ex = assertThrows(UnableExecuteCommandException.class, () -> command.createContext(parameter));
        assertThat(ex.getMessage()).isEqualTo("Cannot execute command 'double'");

        verify(command, atLeastOnce()).prepareContext(doubleCommand, parameter);
        verify(command, never()).prepareContext(booleanCommand, parameter);
        verify(command, never()).prepareContext(intCommand, parameter);
    }

    @Test
    void shouldNotCreateMacroContext_IntContextExceptionThrown() {
        int parameter = 104;
        when(command.prepareContext(intCommand, parameter)).thenThrow(new UnableExecuteCommandException("double"));

        Exception ex = assertThrows(UnableExecuteCommandException.class, () -> command.createContext(parameter));
        assertThat(ex.getMessage()).isEqualTo("Cannot execute command 'double'");

        verify(command).prepareContext(doubleCommand, parameter);
        verify(command, atLeastOnce()).prepareContext(booleanCommand, parameter);
        verify(command, atLeastOnce()).prepareContext(intCommand, parameter);
    }

    @Test
    void shouldDoMacroCommandRedo() {
        int parameter = 105;
        allowCreateRealContexts(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        Context<Integer> macroContext = (Context<Integer>) command.createContext(parameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        assertThat(macroContext.getRedoParameter()).isInstanceOf(CommandParameterWrapper.class);

        command.doCommand(macroContext);

        verify(command).executeDo(macroContext);
        assertThat(macroContext.getState()).isEqualTo(DONE);
        CommandParameterWrapper wrapper = (CommandParameterWrapper) macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        wrapper.getNestedContexts().forEach(ctx -> assertThat(ctx.getState()).isEqualTo(DONE));

        Context<?> doubleContext = wrapper.getNestedContexts().getFirst();
        assertThat(doubleContext.getResult().orElse(null)).isEqualTo(parameter * 100.0);

        Context<?> intContext = wrapper.getNestedContexts().getLast();
        assertThat(intContext.getResult().orElse(null)).isEqualTo(parameter * 10);

        assertThat(macroContext.getResult()).isEqualTo(wrapper.getNestedContexts().getLast().getResult());

        verify(command).redoNestedContexts(eq(wrapper.getNestedContexts()), any(Context.StateChangedListener.class));
        verify(command).redoNestedCommand(eq(doubleContext), any(Context.StateChangedListener.class));
        verify(doubleCommand).doCommand(doubleContext);
        verify(command).redoNestedCommand(eq(intContext), any(Context.StateChangedListener.class));
        verify(intCommand).doCommand(intContext);
    }

    @Test
    void shouldDontMacroCommandRedo_NoNestedContextsChain() {
        int parameter = 106;
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = (Context<Integer>) command.createContext(parameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        assertThat(macroContext.getRedoParameter()).isInstanceOf(CommandParameterWrapper.class);
        CommandParameterWrapper wrapper = (CommandParameterWrapper) macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);

        wrapper.getNestedContexts().clear();

        command.doCommand(macroContext);

        assertThat(macroContext.getState()).isEqualTo(FAIL);
        assertThat(macroContext.getException()).isInstanceOf(NoSuchElementException.class);
        verify(command).executeDo(macroContext);
        verify(command).redoNestedContexts(eq(wrapper.getNestedContexts()), any(Context.StateChangedListener.class));
    }

    @Test
    void shouldDontMacroCommandRedo_NestedContextsAreNull() {
        int parameter = 107;
        Context<Integer> macroContext = (Context<Integer>) command.createContext(parameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        assertThat(macroContext.getRedoParameter()).isInstanceOf(CommandParameterWrapper.class);
        CommandParameterWrapper wrapper = (CommandParameterWrapper) macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);

        command.doCommand(macroContext);

        assertThat(macroContext.getState()).isEqualTo(FAIL);
        assertThat(macroContext.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeDo(macroContext);
        verify(command).redoNestedContexts(eq(wrapper.getNestedContexts()), any(Context.StateChangedListener.class));
    }

    @Test
    void shouldDontMacroCommandRedo_MacroContextThrows() {
        int parameter = 128;
        Context<Integer> macroContext = (Context<Integer>) command.createContext(parameter);
        doThrow(UnableExecuteCommandException.class).when(command).executeDo(macroContext);

        Exception exception = assertThrows(UnableExecuteCommandException.class, () -> command.doCommand(macroContext));

        assertThat(exception).isInstanceOf(UnableExecuteCommandException.class);
        verify(command).executeDo(macroContext);
    }

    @Test
    void shouldDontMacroCommandRedo_LastNestedContextThrows() {
        int parameter = 108;
        allowCreateRealContexts(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        doThrow(UnableExecuteCommandException.class).when(intCommand).doCommand(any(Context.class));
        Context<Integer> macroContext = (Context<Integer>) command.createContext(parameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        assertThat(macroContext.getRedoParameter()).isInstanceOf(CommandParameterWrapper.class);

        command.doCommand(macroContext);

        verify(command).executeDo(macroContext);
        CommandParameterWrapper wrapper = (CommandParameterWrapper) macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        Context<?> doubleContext = wrapper.getNestedContexts().getFirst();
        Context<?> intContext = wrapper.getNestedContexts().getLast();
        assertThat(doubleContext.getState()).isEqualTo(DONE);
        assertThat(doubleContext.getResult().orElse(null)).isEqualTo(parameter * 100.0);
        assertThat(intContext.getState()).isEqualTo(FAIL);
        assertThat(intContext.getException()).isInstanceOf(UnableExecuteCommandException.class);

        verify(command).redoNestedContexts(eq(wrapper.getNestedContexts()), any(Context.StateChangedListener.class));

        verify(command).redoNestedCommand(eq(doubleContext), any(Context.StateChangedListener.class));
        verify(doubleCommand).doCommand(doubleContext);
        verify(command).redoNestedCommand(eq(intContext), any(Context.StateChangedListener.class));
        verify(intCommand).doCommand(intContext);

        assertThat(macroContext.getState()).isEqualTo(FAIL);
        assertThat(macroContext.getException()).isEqualTo(intContext.getException());
        verify(command).rollbackNestedDoneContexts(any(Deque.class));
        verify(doubleCommand).undoCommand(wrapper.getNestedContexts().pop());
        verify(booleanCommand).undoCommand(wrapper.getNestedContexts().pop());
    }

    @Test
    void shouldDontMacroCommandRedo_FirstNestedContextThrows() {
        int parameter = 109;
        allowCreateRealContexts(parameter);
        doThrow(UnableExecuteCommandException.class).when(doubleCommand).doCommand(any(Context.class));
        configureNestedRedoResult(intCommand, parameter * 10);
        configureNestedRedoResult(booleanCommand, true);
        Context<Integer> macroContext = (Context<Integer>) command.createContext(parameter);
        assertThat(macroContext).isNotNull();
        assertThat(macroContext.getState()).isEqualTo(READY);
        assertThat(macroContext.getCommand()).isEqualTo(command);
        assertThat(macroContext.getRedoParameter()).isInstanceOf(CommandParameterWrapper.class);

        command.doCommand(macroContext);

        verify(command).executeDo(macroContext);
        CommandParameterWrapper wrapper = (CommandParameterWrapper) macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        Context<?> doubleContext = wrapper.getNestedContexts().getFirst();
        Context<?> intContext = wrapper.getNestedContexts().getLast();
        assertThat(doubleContext.getState()).isEqualTo(FAIL);
        assertThat(doubleContext.getException()).isInstanceOf(UnableExecuteCommandException.class);
        assertThat(intContext.getState()).isEqualTo(DONE);
        assertThat(intContext.getResult().orElse(null)).isEqualTo(parameter * 10);

        verify(command).redoNestedContexts(any(Deque.class), any(Context.StateChangedListener.class));

        verify(command).redoNestedCommand(eq(doubleContext), any(Context.StateChangedListener.class));
        verify(doubleCommand).doCommand(doubleContext);
        verify(command).redoNestedCommand(eq(intContext), any(Context.StateChangedListener.class));
        verify(intCommand).doCommand(intContext);

        assertThat(macroContext.getState()).isEqualTo(FAIL);
        assertThat(macroContext.getException()).isEqualTo(doubleContext.getException());
        verify(command).rollbackNestedDoneContexts(any(Deque.class));
        wrapper.getNestedContexts().pop();
        verify(booleanCommand).undoCommand(wrapper.getNestedContexts().pop());
        verify(intCommand).undoCommand(wrapper.getNestedContexts().pop());
    }

    @Test
    void shouldDoMacroCommandUndo() {
        int parameter = 110;
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = (Context<Integer>) command.createContext(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        command.doCommand(macroContext);
        assertThat(macroContext.getState()).isEqualTo(DONE);
        Deque<Context<?>> nestedDoneContexts = (Deque<Context<?>>) macroContext.getUndoParameter();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.getState()).isEqualTo(DONE));
        configureNestedUndoStatus(doubleCommand);
        configureNestedUndoStatus(booleanCommand);
        configureNestedUndoStatus(intCommand);

        command.undoCommand(macroContext);

        assertThat(macroContext.getState()).isEqualTo(UNDONE);
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.getState()).isEqualTo(UNDONE));

        verify(command).executeUndo(macroContext);
        verify(command).rollbackNestedDoneContexts(nestedDoneContexts);
        nestedDoneContexts.forEach(ctx -> verify(ctx.getCommand()).undoCommand(ctx));
    }

    @Test
    void shouldDontMacroCommandUndo_MacroContextThrown() {
        int parameter = 111;
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = (Context<Integer>) command.createContext(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        command.doCommand(macroContext);
        assertThat(macroContext.getState()).isEqualTo(DONE);
        Deque<Context<?>> nestedDoneContexts = (Deque<Context<?>>) macroContext.getUndoParameter();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.getState()).isEqualTo(DONE));
        doThrow(UnableExecuteCommandException.class).when(command).executeUndo(macroContext);

        Exception exception = assertThrows(UnableExecuteCommandException.class, () -> command.undoCommand(macroContext));

        assertThat(exception).isInstanceOf(UnableExecuteCommandException.class);
        verify(command).executeUndo(macroContext);
        verify(command, never()).rollbackNestedDoneContexts(nestedDoneContexts);
    }

    @Test
    void shouldDontMacroCommandUndo_NestedContextThrown() {
        int parameter = 112;
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = (Context<Integer>) command.createContext(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        command.doCommand(macroContext);
        assertThat(macroContext.getState()).isEqualTo(DONE);
        Deque<Context<?>> nestedDoneContexts = (Deque<Context<?>>) macroContext.getUndoParameter();
        nestedDoneContexts.forEach(ctx -> assertThat(ctx.getState()).isEqualTo(DONE));
        configureNestedUndoStatus(doubleCommand);
        doThrow(UnableExecuteCommandException.class).when(booleanCommand).undoCommand(any(Context.class));
        configureNestedUndoStatus(intCommand);

        command.undoCommand(macroContext);

        verify(command).executeUndo(macroContext);
        verify(command).rollbackNestedDoneContexts(nestedDoneContexts);
        Context current = nestedDoneContexts.pop();
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

        Context doubleContext = command.prepareContext(doubleCommand, parameter);

        assertThat(doubleContext).isNotNull();
        assertThat(doubleContext.getCommand()).isEqualTo(doubleCommand);
        assertThat(doubleContext.getRedoParameter()).isEqualTo(parameter);
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
    void shouldRunNestedContexts() {
        int parameter = 115;
        AtomicInteger counter = new AtomicInteger(0);
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = (Context<Integer>) command.createContext(parameter);
        CommandParameterWrapper wrapper = (CommandParameterWrapper) macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        configureNestedRedoResult(intCommand, parameter * 10);
        Context.StateChangedListener listener = (context, previous, newOne) -> {
            if (newOne == DONE) {
                counter.incrementAndGet();
            }
        };

        command.redoNestedContexts(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isEqualTo(3);
        wrapper.getNestedContexts().forEach(ctx -> {
            verify(ctx.getCommand()).doCommand(ctx);
            assertThat(ctx.getState()).isEqualTo(DONE);
        });
    }

    @Test
    void shouldNotRunNestedContexts_NestedRedoThrows() {
        int parameter = 116;
        AtomicInteger counter = new AtomicInteger(0);
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = (Context<Integer>) command.createContext(parameter);
        CommandParameterWrapper wrapper = (CommandParameterWrapper) macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        configureNestedRedoResult(booleanCommand, true);
        doThrow(UnableExecuteCommandException.class).when(intCommand).doCommand(any(Context.class));
        Context.StateChangedListener listener = (context, previous, newOne) -> {
            if (newOne == DONE) {
                counter.incrementAndGet();
            }
        };

        command.redoNestedContexts(wrapper.getNestedContexts(), listener);

        assertThat(counter.get()).isEqualTo(2);
        Context current = wrapper.getNestedContexts().pop();
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
    void shouldRunNestedCommand() {
        int parameter = 117;
        AtomicInteger counter = new AtomicInteger(0);
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = (Context<Integer>) command.createContext(parameter);
        CommandParameterWrapper wrapper = (CommandParameterWrapper) macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        configureNestedRedoResult(doubleCommand, parameter * 100.0);
        Context.StateChangedListener listener = (context, previous, newOne) -> {
            if (newOne == DONE) {
                counter.incrementAndGet();
            }
        };

        Context<?> done = command.redoNestedCommand(wrapper.getNestedContexts().getFirst(), listener);

        assertThat(counter.get()).isEqualTo(1);
        verify(done.getCommand()).doCommand(done);
        assertThat(done.getState()).isEqualTo(DONE);

    }

    @Test
    void shouldNotRunNestedCommand_EmptyContext() {
        int parameter = 118;
        AtomicInteger counter = new AtomicInteger(0);
        Context<Integer> macroContext = (Context<Integer>) command.createContext(parameter);
        CommandParameterWrapper wrapper = (CommandParameterWrapper) macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        assertThat(wrapper.getNestedContexts().getFirst()).isNull();
        Context.StateChangedListener listener = (context, previous, newOne) -> {
        };

        assertThrows(NullPointerException.class, () -> command.redoNestedCommand(null, listener));

        assertThat(counter.get()).isZero();
    }

    @Test
    void shouldNotRunNestedCommand_NestedContextRedoThrows() {
        int parameter = 119;
        AtomicInteger counter = new AtomicInteger(0);
        allowCreateRealContexts(parameter);
        Context<Integer> macroContext = (Context<Integer>) command.createContext(parameter);
        CommandParameterWrapper wrapper = (CommandParameterWrapper) macroContext.getRedoParameter();
        assertThat(wrapper.getInput()).isEqualTo(parameter);
        doThrow(UnableExecuteCommandException.class).when(doubleCommand).doCommand(any(Context.class));
        Context.StateChangedListener listener = (context, previous, newOne) -> {
            if (newOne == DONE) {
                counter.incrementAndGet();
            }
        };

        Context<?> done = command.redoNestedCommand(wrapper.getNestedContexts().getFirst(), listener);

        assertThat(counter.get()).isZero();
        assertThat(done.getState()).isEqualTo(FAIL);
        assertThat(done.getException()).isInstanceOf(UnableExecuteCommandException.class);
    }

    private static class FakeMacroCommand extends MacroCommand {
        Logger logger = LoggerFactory.getLogger(FakeMacroCommand.class);

        @Override
        public Logger getLog() {
            return logger;
        }

        public String getId() {
            return "fake-command";
        }
    }

    private void allowCreateRealContexts(Object parameter) {
        doCallRealMethod().when(doubleCommand).createContext(parameter);
        doCallRealMethod().when(booleanCommand).createContext(parameter);
        doCallRealMethod().when(intCommand).createContext(parameter);
    }

    private <T> void configureNestedRedoResult(SchoolCommand nextedCommand, T result) {
        doAnswer(invocationOnMock -> {
            Context<?> context = invocationOnMock.getArgument(0, Context.class);
            context.setState(WORK);
            context.setResult(result);
            return null;
        }).when(nextedCommand).doCommand(any(Context.class));
    }

    private <T> void configureNestedUndoStatus(SchoolCommand nextedCommand) {
        doAnswer(invocationOnMock -> {
            Context<?> context = invocationOnMock.getArgument(0, Context.class);
            context.setState(WORK);
            context.setState(UNDONE);
            return null;
        }).when(nextedCommand).undoCommand(any(Context.class));
    }
}