package oleg.sopilnyak.test.service.command.executable.profile;

import oleg.sopilnyak.test.school.common.exception.ProfileNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.school.common.model.PersonProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteProfileCommandTest {
    @Mock
    ProfilePersistenceFacade persistenceFacade;
    @Spy
    @InjectMocks
    DeleteProfileCommand command;

    @Mock
    PersonProfile input;
    @Mock
    StudentProfile profile;

    @Test
    void shouldExecuteCommand() throws ProfileNotExistsException {
        long id = 404L;
        when(persistenceFacade.findProfileById(id)).thenReturn(Optional.of(input));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findProfileById(id);
        verify(persistenceFacade).deleteProfileById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isTrue();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand_ProfileNotExists() throws ProfileNotExistsException {
        long id = 405L;

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findProfileById(id);
        verify(persistenceFacade, never()).deleteProfileById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isFalse();
        assertThat(result.getException()).isInstanceOf(ProfileNotExistsException.class);
    }

    @Test
    void shouldNotExecuteCommand_WrongIdType() {

        CommandResult<Boolean> result = command.execute("id");

        verify(persistenceFacade, never()).findProfileById(anyLong());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isFalse();
        assertThat(result.getException()).isInstanceOf(ClassCastException.class);
    }

    @Test
    void shouldNotExecuteCommand_NullId() throws ProfileNotExistsException {
        when(persistenceFacade.findProfileById(null)).thenReturn(Optional.of(input));
        doThrow(new RuntimeException()).when(persistenceFacade).deleteProfileById(null);

        CommandResult<Boolean> result = command.execute(null);

        verify(persistenceFacade).findProfileById(null);
        verify(persistenceFacade).deleteProfileById(null);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isFalse();
        assertThat(result.getException()).isInstanceOf(RuntimeException.class);
    }

    @Test
    void shouldNotExecuteCommand_ExceptionThrown() throws ProfileNotExistsException {
        long id = 405L;
        when(persistenceFacade.findProfileById(id)).thenReturn(Optional.of(input));
        doThrow(new UnsupportedOperationException()).when(persistenceFacade).deleteProfileById(id);

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findProfileById(id);
        verify(persistenceFacade).deleteProfileById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isFalse();
        assertThat(result.getException()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldExecuteCommandRedo() throws ProfileNotExistsException {
        long id = 414L;
        when(persistenceFacade.toEntity(profile)).thenReturn(profile);
        when(persistenceFacade.findProfileById(id)).thenReturn(Optional.of(profile));
        Context<Boolean> context = command.createContext(id);

        command.redo(context);

        assertThat(context.getResult()).contains(true);
        assertThat(context.getUndoParameter()).isEqualTo(profile);
        assertThat(context.getState()).isEqualTo(Context.State.DONE);
        assertThat(context.getException()).isNull();

        verify(command).doRedo(context);
        verify(persistenceFacade).findProfileById(id);
        verify(persistenceFacade).deleteProfileById(id);
    }

    @Test
    void shouldNotExecuteCommandRedo_NoProfile() throws ProfileNotExistsException {
        long id = 415L;
        Context<Boolean> context = command.createContext(id);

        command.redo(context);

        assertThat(context.getResult()).contains(false);
        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(ProfileNotExistsException.class);

        verify(command).doRedo(context);
        verify(persistenceFacade).findProfileById(id);
        verify(persistenceFacade, never()).deleteProfileById(id);
    }

    @Test
    void shouldNotExecuteCommandRedo_WrongParameterType() throws ProfileNotExistsException {
        Context<Boolean> context = command.createContext("id");

        command.redo(context);

        assertThat(context.getResult()).contains(false);
        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);

        verify(command).doRedo(context);
        verify(persistenceFacade, never()).findProfileById(anyLong());
        verify(persistenceFacade, never()).deleteProfileById(anyLong());
    }

    @Test
    void shouldNotExecuteCommandRedo_ExceptionThrown() throws ProfileNotExistsException {
        long id = 416L;
        when(persistenceFacade.toEntity(profile)).thenReturn(profile);
        when(persistenceFacade.findProfileById(id)).thenReturn(Optional.of(profile));
        doThrow(new UnsupportedOperationException()).when(persistenceFacade).deleteProfileById(id);
        Context<Boolean> context = command.createContext(id);

        command.redo(context);

        assertThat(context.getResult()).contains(false);
        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(UnsupportedOperationException.class);

        verify(command).doRedo(context);
        verify(persistenceFacade).findProfileById(id);
        verify(persistenceFacade).deleteProfileById(id);
    }

    @Test
    void shouldExecuteCommandUndo() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(profile);

        command.undo(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        assertThat(context.getException()).isNull();

        verify(command).doUndo(context);
        verify(persistenceFacade).saveProfile(profile);
    }

    @Test
    void shouldNotExecuteCommandUndo_WrongUndoParameter() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter("input");

        command.undo(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);

        verify(command).doUndo(context);
        verify(persistenceFacade, never()).saveProfile(any(PersonProfile.class));
    }

    @Test
    void shouldNotExecuteCommandUndo_ExceptionThrown() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(input);
        doThrow(new UnsupportedOperationException()).when(persistenceFacade).saveProfile(input);

        command.undo(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(UnsupportedOperationException.class);

        verify(command).doUndo(context);
        verify(persistenceFacade).saveProfile(input);
    }
}