package oleg.sopilnyak.test.service.command.executable.profile.student;

import oleg.sopilnyak.test.school.common.exception.profile.ProfileIsNotFoundException;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.exception.InvalidParameterTypeException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.StudentProfilePayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteStudentProfileCommandTest {
    @Mock
    StudentProfile profile;
    @Mock
    StudentProfilePayload payload;
    @Mock
    ProfilePersistenceFacade persistence;
    @Mock
    BusinessMessagePayloadMapper payloadMapper;
    @Spy
    @InjectMocks
    DeleteStudentProfileCommand command;

    @Test
    void shouldBeValidCommand() {
        assertThat(command).isNotNull();
        assertThat(persistence).isEqualTo(ReflectionTestUtils.getField(command, "persistence"));
        assertThat(payloadMapper).isEqualTo(ReflectionTestUtils.getField(command, "payloadMapper"));
    }

    @Test
    void shouldWorkFunctionFindById() {
        Long id = 413L;
        doCallRealMethod().when(persistence).findStudentProfileById(id);

        command.functionFindById().apply(id);

        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldWorkFunctionCopyEntity() {
        when(persistence.toEntity(profile)).thenReturn(profile);
        command.functionAdoptEntity().apply(profile);

        verify(persistence).toEntity(profile);
        verify(payloadMapper).toPayload(profile);
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
        doCallRealMethod().when(persistence).findStudentProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        when(payloadMapper.toPayload(profile)).thenReturn(payload);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getException()).isNull();
        assertThat(context.getResult()).contains(true);
        assertThat(context.<Object>getUndoParameter()).isEqualTo(payload);
        verify(command).executeDo(context);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(profile);
        verify(payloadMapper).toPayload(profile);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldNotDoCommand_NoProfile() {
        long id = 415L;
        doCallRealMethod().when(persistence).findStudentProfileById(id);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileIsNotFoundException.class);
        assertThat(context.getException().getMessage()).startsWith("Profile with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence, never()).toEntity(any(StudentProfile.class));
        verify(payloadMapper, never()).toPayload(any(StudentProfile.class));
        verify(persistence, never()).deleteProfileById(id);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() throws ProfileIsNotFoundException {
        Context<Boolean> context = command.createContext("id");

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
        verify(persistence, never()).findProfileById(anyLong());
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() throws ProfileIsNotFoundException {
        long id = 416L;
        when(persistence.toEntity(profile)).thenReturn(profile);
        doCallRealMethod().when(persistence).findStudentProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(profile));
        doThrow(new UnsupportedOperationException()).when(persistence).deleteProfileById(id);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(UnsupportedOperationException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).deleteProfileById(id);
    }

    @Test
    void shouldUndoCommand_UndoProfileExists() {
        doCallRealMethod().when(persistence).save(profile);
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(profile);
        when(persistence.saveProfile(profile)).thenReturn(Optional.of(profile));

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
    }

    @Test
    void shouldNotUndoCommand_WrongUndoCommandParameterType() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter("input");

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Parameter not a  'StudentProfile' value:[input]");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(profile);
    }

    @Test
    void shouldNotUndoCommand_NullUndoCommandParameter() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(null);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong input parameter value null");
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