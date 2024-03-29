package oleg.sopilnyak.test.service.command.executable.profile;

import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrUpdateProfileCommandTest {
    @Mock
    ProfilePersistenceFacade persistenceFacade;
    @InjectMocks
    CreateOrUpdateProfileCommand command;
    @Mock
    PersonProfile input;

    @Test
    void shouldExecuteCommand() {

        CommandResult<Optional<PersonProfile>> result = command.execute(input);

        verify(persistenceFacade).saveProfile(input);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        RuntimeException cannotExecute = new RuntimeException("Cannot update");
        doThrow(cannotExecute).when(persistenceFacade).saveProfile(input);


        CommandResult<Optional<PersonProfile>> result = command.execute(input);

        verify(persistenceFacade).saveProfile(input);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }

    @Test
    void shouldExecuteRedoCommand_ExistsProfileId() {
        Long id = 700L;
        when(input.getId()).thenReturn(id);
        Context<Optional<PersonProfile>> context = command.createContext(input);
        when(persistenceFacade.findProfileById(id)).thenReturn(Optional.of(input));
        when(persistenceFacade.saveProfile(input)).thenReturn(Optional.of(input));

        command.redo(context);

        assertThat(context.getState()).isEqualTo(Context.State.DONE);
        assertThat(context.getUndoParameter()).isEqualTo(input);
    }

    @Test
    void shouldExecuteRedoCommand_NotExistsProfileId() {
        Long id = -700L;
        when(input.getId()).thenReturn(id);
        Context<Optional<PersonProfile>> context = command.createContext(input);
        when(persistenceFacade.saveProfile(input)).thenReturn(Optional.of(input));

        command.redo(context);

        assertThat(context.getState()).isEqualTo(Context.State.DONE);
        assertThat(context.getUndoParameter()).isEqualTo(id);
    }

    @Test
    void shouldNotExecuteRedoCommand_WrongState() {
        Context<Optional<PersonProfile>> context = command.createContext();

        command.redo(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
    }

    @Test
    void shouldNotExecuteRedoCommand_NotSaved() {
        Context<Optional<PersonProfile>> context = command.createContext(input);
        when(persistenceFacade.saveProfile(input)).thenReturn(Optional.empty());

        command.redo(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
    }

    @Test
    void shouldNotExecuteRedoCommand_SaveExceptionThrown() {
        Context<Optional<PersonProfile>> context = command.createContext(input);
        when(persistenceFacade.saveProfile(input)).thenThrow(new RuntimeException());

        command.redo(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldNotExecuteRedoCommand_FindEmptyResult() {
        Long id = 701L;
        when(input.getId()).thenReturn(id);
        Context<Optional<PersonProfile>> context = command.createContext(input);
        when(persistenceFacade.findProfileById(id)).thenReturn(Optional.empty());

        command.redo(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(ProfileNotExistsException.class);
    }

    @Test
    void shouldNotExecuteRedoCommand_FindExceptionThrown() {
        Long id = 702L;
        when(input.getId()).thenReturn(id);
        Context<Optional<PersonProfile>> context = command.createContext(input);
        when(persistenceFacade.findProfileById(id)).thenThrow(new RuntimeException());

        command.redo(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldExecuteUndoCommand_ExistsProfile() {
        Long id = 700L;
        when(input.getId()).thenReturn(id);
        Context<Optional<PersonProfile>> context = command.createContext(input);
        when(persistenceFacade.findProfileById(id)).thenReturn(Optional.of(input));
        when(persistenceFacade.saveProfile(input)).thenReturn(Optional.of(input));
        command.redo(context);
        assertThat(context.getState()).isEqualTo(Context.State.DONE);
        assertThat(context.getUndoParameter()).isEqualTo(input);

        command.undo(context);

        verify(persistenceFacade, atLeastOnce()).saveProfile(input);
        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
    }

    @Test
    void shouldExecuteUndoCommand_NotWrongProfileId() throws ProfileNotExistsException {
        Long id = -700L;
        when(input.getId()).thenReturn(id);
        Context<Optional<PersonProfile>> context = command.createContext(input);
        when(persistenceFacade.saveProfile(input)).thenReturn(Optional.of(input));
        command.redo(context);
        assertThat(context.getState()).isEqualTo(Context.State.DONE);
        assertThat(context.getUndoParameter()).isEqualTo(id);

        command.undo(context);

        verify(persistenceFacade, atLeastOnce()).saveProfile(input);
        verify(persistenceFacade).deleteProfileById(id);
        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
    }

    @Test
    void shouldNotExecuteUndoCommand_WrongState() {
        Context<Optional<PersonProfile>> context = command.createContext();

        command.undo(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
    }

    @Test
    void shouldNotExecuteUndoCommand_WrongUndoParameter() {
        Context<Optional<PersonProfile>> context = command.createContext();
        context.setState(Context.State.DONE);

        command.undo(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldNotExecuteUndoCommand_DeleteByIdExceptionThrown() throws ProfileNotExistsException {
        Long id = 800L;
        Context<Optional<PersonProfile>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(id);
        doThrow(new RuntimeException()).when(persistenceFacade).deleteProfileById(id);

        command.undo(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldNotExecuteUndoCommand_SaveProfileExceptionThrown() {
        Context<Optional<PersonProfile>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(input);
        doThrow(new RuntimeException()).when(persistenceFacade).saveProfile(input);

        command.undo(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
    }
}