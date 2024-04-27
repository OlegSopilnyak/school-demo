package oleg.sopilnyak.test.service.command.executable.profile.principal;

import oleg.sopilnyak.test.school.common.exception.EntityNotExistException;
import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrUpdatePrincipalProfileCommandTest {
    @Mock
    ProfilePersistenceFacade persistence;
    @Spy
    @InjectMocks
    CreateOrUpdatePrincipalProfileCommand command;
    @Mock
    PrincipalProfile profile;

    @Test
    void shouldWorkFunctionFindById() {
        Long id = 710L;
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
    void shouldDoCommand_UpdateProfile() {
        Long id = 700L;
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        doCallRealMethod().when(persistence).save(profile);
        when(profile.getId()).thenReturn(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        when(persistence.saveProfile(profile)).thenReturn(Optional.of(profile));
        Context<Optional<PrincipalProfile>> context = command.createContext(profile);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        Optional<PrincipalProfile> doResult = (Optional<PrincipalProfile>) context.getResult().orElseThrow();
        assertThat(doResult.orElseThrow()).isEqualTo(profile);
        assertThat(context.getUndoParameter()).isEqualTo(profile);
        verify(command).executeDo(context);
        verify(profile).getId();
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    void shouldDoCommand_CreateProfile() {
        doCallRealMethod().when(persistence).save(profile);
        Long id = -700L;
        when(profile.getId()).thenReturn(id);
        Context<Optional<PrincipalProfile>> context = command.createContext(profile);
        when(persistence.saveProfile(profile)).thenReturn(Optional.of(profile));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter()).isEqualTo(id);
        Optional<PrincipalProfile> doResult = (Optional<PrincipalProfile>) context.getResult().orElseThrow();
        assertThat(doResult.orElseThrow()).isEqualTo(profile);
        verify(command).executeDo(context);
        verify(profile, times(2)).getId();
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    void shouldNotDoCommand_WrongState() {
        Context<Optional<PrincipalProfile>> context = command.createContext();

        command.doCommand(context);

        assertThat(context.getResult()).isEmpty();
        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_IncompatibleProfileType() {
        Context<Optional<PrincipalProfile>> context = command.createContext(mock(StudentProfile.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isInstanceOf(EntityNotExistException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong type of PrincipalProfile:");

        verify(command).executeDo(context);
        verify(persistence, never()).saveProfile(any());
    }

    @Test
    void shouldNotDoCommand_ProfileNotFound() {
        Long id = 701L;
        when(profile.getId()).thenReturn(id);
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        Context<Optional<PrincipalProfile>> context = command.createContext(profile);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistProfileException.class);
        assertThat(context.getException().getMessage()).startsWith("Profile with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(profile).getId();
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).saveProfile(any());
    }

    @Test
    void shouldNotDoCommand_FindExceptionThrown() {
        Long id = 702L;
        when(profile.getId()).thenReturn(id);
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        doThrow(RuntimeException.class).when(persistence).findProfileById(id);
        Context<Optional<PrincipalProfile>> context = command.createContext(profile);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(profile).getId();
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).saveProfile(any());
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<PrincipalProfile>> context = command.createContext("input");

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<PrincipalProfile>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldUndoCommand_DeleteCreated() {
        Long id = 704L;
        Context<Optional<PrincipalProfile>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setUndoParameter(id);
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldUndoCommand_RestoreUpdated() {
        doCallRealMethod().when(persistence).save(profile);
        Context<Optional<PrincipalProfile>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setUndoParameter(profile);
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    void shouldNotUndoCommand_WrongState() {
        Context<Optional<PrincipalProfile>> context = command.createContext();

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_EmptyParameter() {
        Context<Optional<PrincipalProfile>> context = command.createContext();
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistProfileException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong undo parameter :");
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Optional<PrincipalProfile>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setUndoParameter("param");
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(NotExistProfileException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong undo parameter :");
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_DeleteByIdExceptionThrown() throws NotExistProfileException {
        Long id = 703L;
        Context<Optional<PrincipalProfile>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setUndoParameter(id);
        context.setState(Context.State.DONE);
        doThrow(new RuntimeException()).when(persistence).deleteProfileById(id);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.FAIL);
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldNotUndoCommand_SaveProfileExceptionThrown() {
        Context<Optional<PrincipalProfile>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setUndoParameter(profile);
        context.setState(Context.State.DONE);
        doCallRealMethod().when(persistence).save(profile);
        doThrow(new RuntimeException()).when(persistence).saveProfile(profile);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }
}