package oleg.sopilnyak.test.service.command.executable.profile.principal;

import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeletePrincipalProfileCommandTest {
    @Mock
    ProfilePersistenceFacade persistence;
    @Spy
    @InjectMocks
    DeletePrincipalProfileCommand command;
    @Mock
    PrincipalProfile profile;

    @Test
    void shouldWorkFunctionFindById() {
        Long id = 413L;
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);

        command.functionFindById().apply(id);

        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldWorkFunctionCopyEntity() {
        command.functionCopyEntity().apply(profile);

        verify(persistence).toEntity(profile);
    }

    @Test
    void shouldWorkFunctionSave() {
        doCallRealMethod().when(persistence).save(profile);

        command.functionSave().apply(profile);

        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    void shouldDoCommand_ProfileExists() {
        long id = 414L;
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getException()).isNull();
        assertThat(context.getResult()).contains(true);
        assertThat(context.getUndoParameter()).isEqualTo(profile);
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldNotDoCommand_NoProfile() {
        long id = 415L;
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistProfileException.class);
        assertThat(context.getException().getMessage()).startsWith("Profile with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).deleteProfileById(id);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() throws NotExistProfileException {
        Context<Boolean> context = command.createContext("id");

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
        verify(persistence, never()).findProfileById(anyLong());
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() throws NotExistProfileException {
        long id = 416L;
        when(persistence.toEntity(profile)).thenReturn(profile);
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(profile));
        doThrow(new UnsupportedOperationException()).when(persistence).deleteProfileById(id);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(UnsupportedOperationException.class);
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldUndoCommand_UndoProfileExists() {
        doCallRealMethod().when(persistence).save(profile);
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(profile);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    void shouldUndoCommand_WrongUndoCommandParameterType() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter("input");

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence, never()).save(profile);
    }

    @Test
    void shouldUndoCommand_NullUndoCommandParameter() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(null);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence, never()).save(profile);
    }

    @Test
    void shouldNotUndoCommand_ExceptionThrown() {
        doCallRealMethod().when(persistence).save(profile);
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(profile);
        doThrow(new UnsupportedOperationException()).when(persistence).saveProfile(profile);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(UnsupportedOperationException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }
}