package oleg.sopilnyak.test.service.command.executable.profile;

import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindProfileCommandTest {
    @Mock
    ProfilePersistenceFacade persistenceFacade;
    @Spy
    @InjectMocks
    FindProfileCommand command;
    @Mock
    PersonProfile profile;

    @Test
    void shouldExecuteCommand() {
        Long id = 402L;

        CommandResult<Optional<PersonProfile>> result = command.execute(id);

        verify(persistenceFacade).findProfileById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand_NoProfile() {
        Long id = 413L;

        CommandResult<Optional<PersonProfile>> result = command.execute(id);

        verify(persistenceFacade).findProfileById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
    }

    @Test
    void shouldNotExecuteCommand_ExceptionThrown() {
        Long id = 403L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findProfileById(id);

        CommandResult<Optional<PersonProfile>> result = command.execute(id);

        verify(persistenceFacade).findProfileById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }

    @Test
    void shouldExecuteCommandDoCommand() {
        Long id = 404L;
        when(persistenceFacade.findProfileById(id)).thenReturn(Optional.of(profile));
        Context<Optional<PersonProfile>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.getResult()).contains(Optional.of(profile));
        assertThat(context.getState()).isEqualTo(DONE);
        assertThat(context.getException()).isNull();

        verify(command).doRedo(context);
        verify(persistenceFacade).findProfileById(id);
    }

    @Test
    void shouldNotExecuteCommandDoCommand_NotFound() {
        Long id = 405L;
        Context<Optional<PersonProfile>> context = command.createContext(id);

        command.doCommand(context);

        assertThat((Optional) context.getResult().orElse(Optional.empty())).isEmpty();
        assertThat(context.getState()).isEqualTo(DONE);
        assertThat(context.getException()).isNull();

        verify(command).doRedo(context);
        verify(persistenceFacade).findProfileById(id);
    }

    @Test
    void shouldNotExecuteCommandDoCommand_WrongParameterType() {
        Long id = 406L;
        Context<Optional<PersonProfile>> context = command.createContext("" + id);

        command.doCommand(context);

        assertThat(context.getResult()).isEmpty();
        assertThat(context.getState()).isEqualTo(FAIL);
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);

        verify(command).doRedo(context);
        verify(persistenceFacade, never()).findProfileById(id);
    }

    @Test
    void shouldExecuteCommandUndoCommand() {
        Long id = 414L;
        Context<Optional<PersonProfile>> context = command.createContext(id);
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();

        verify(command).doUndo(context);
    }
}