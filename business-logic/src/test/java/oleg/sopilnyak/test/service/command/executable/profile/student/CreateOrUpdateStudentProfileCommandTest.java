package oleg.sopilnyak.test.service.command.executable.profile.student;

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrUpdateStudentProfileCommandTest {
    @Mock
    ProfilePersistenceFacade persistence;
    @Spy
    @InjectMocks
    CreateOrUpdateStudentProfileCommand command;
    @Mock
    StudentProfile profile;

    @Test
    void shouldWorkFunctionFindById() {
        Long id = 810L;
        doCallRealMethod().when(persistence).findStudentProfileById(id);

        command.functionFindById().apply(id);

        verify(persistence).findStudentProfileById(id);
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
        Long id = 800L;
        doCallRealMethod().when(persistence).findStudentProfileById(id);
        doCallRealMethod().when(persistence).save(profile);
        when(profile.getId()).thenReturn(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        when(persistence.saveProfile(profile)).thenReturn(Optional.of(profile));
        Context<Optional<StudentProfile>> context = command.createContext(profile);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        Optional<StudentProfile> doResult = context.getResult().orElseThrow();
        assertThat(doResult.orElseThrow()).isEqualTo(profile);
        assertThat(context.<Object>getUndoParameter()).isEqualTo(profile);
        verify(command).executeDo(context);
        verify(profile).getId();
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    void shouldDoCommand_CreateProfile() {
        doCallRealMethod().when(persistence).save(profile);
        Long id = -800L;
        when(profile.getId()).thenReturn(id);
        Context<Optional<StudentProfile>> context = command.createContext(profile);
        when(persistence.saveProfile(profile)).thenReturn(Optional.of(profile));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isEqualTo(id);
        Optional<StudentProfile> doResult = context.getResult().orElseThrow();
        assertThat(doResult.orElseThrow()).isEqualTo(profile);
        verify(command).executeDo(context);
        verify(profile, times(2)).getId();
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    void shouldNotDoCommand_WrongState() {
        Context<Optional<StudentProfile>> context = command.createContext();

        command.doCommand(context);

        assertThat(context.getResult()).isEmpty();
        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_IncompatibleProfileType() {
        Context<Optional<StudentProfile>> context = command.createContext(mock(PrincipalProfile.class));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getException()).isInstanceOf(EntityNotExistException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong type of StudentProfile:");

        verify(command).executeDo(context);
        verify(persistence, never()).saveProfile(any());
    }

    @Test
    void shouldNotDoCommand_ProfileNotFound() {
        Long id = 801L;
        when(profile.getId()).thenReturn(id);
        doCallRealMethod().when(persistence).findStudentProfileById(id);
        Context<Optional<StudentProfile>> context = command.createContext(profile);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistProfileException.class);
        assertThat(context.getException().getMessage()).startsWith("Profile with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(profile).getId();
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).saveProfile(any());
    }

    @Test
    void shouldNotDoCommand_FindUpdatedExceptionThrown() {
        Long id = 802L;
        when(profile.getId()).thenReturn(id);
        doCallRealMethod().when(persistence).findStudentProfileById(id);
        doThrow(RuntimeException.class).when(persistence).findProfileById(id);
        Context<Optional<StudentProfile>> context = command.createContext(profile);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(profile).getId();
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).saveProfile(any());
    }

    @Test
    void shouldNotDoCommand_SaveCreatedExceptionThrown() {
        doCallRealMethod().when(persistence).save(profile);
        doThrow(RuntimeException.class).when(persistence).saveProfile(profile);
        Context<Optional<StudentProfile>> context = command.createContext(profile);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(profile).getId();
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    void shouldNotDoCommand_SaveUpdatedExceptionThrown() {
        Long id = 803L;
        when(profile.getId()).thenReturn(id);
        doCallRealMethod().when(persistence).findStudentProfileById(id);
        doCallRealMethod().when(persistence).save(profile);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        doThrow(RuntimeException.class).when(persistence).saveProfile(profile);
        Context<Optional<StudentProfile>> context = command.createContext(profile);

        assertThrows(RuntimeException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(profile).getId();
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(profile);
        verify(persistence, times(2)).save(profile);
        verify(persistence, times(2)).saveProfile(profile);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<StudentProfile>> context = command.createContext("input");

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<StudentProfile>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldUndoCommand_DeleteCreated() {
        Long id = 804L;
        Context<Optional<StudentProfile>> context = command.createContext();
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
        Context<Optional<StudentProfile>> context = command.createContext();
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
        Context<Optional<StudentProfile>> context = command.createContext();

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_EmptyParameter() {
        Context<Optional<StudentProfile>> context = command.createContext();
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistProfileException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong undo parameter :");
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Optional<StudentProfile>> context = command.createContext();
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
        Context<Optional<StudentProfile>> context = command.createContext();
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
        Context<Optional<StudentProfile>> context = command.createContext();
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