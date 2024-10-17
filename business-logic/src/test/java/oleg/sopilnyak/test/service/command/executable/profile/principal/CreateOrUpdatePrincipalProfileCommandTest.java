package oleg.sopilnyak.test.service.command.executable.profile.principal;

import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.model.StudentProfile;
import oleg.sopilnyak.test.school.common.persistence.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.exception.InvalidParameterTypeException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.PrincipalProfilePayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.NoSuchAlgorithmException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrUpdatePrincipalProfileCommandTest {
    @Mock
    PrincipalProfile profile;
    @Mock
    PrincipalProfilePayload payload;
    @Mock
    ProfilePersistenceFacade persistence;
    @Mock
    BusinessMessagePayloadMapper payloadMapper;
    @Spy
    @InjectMocks
    CreateOrUpdatePrincipalProfileCommand command;

    @Test
    void shouldBeValidCommand() {
        assertThat(command).isNotNull();
        assertThat(persistence).isEqualTo(ReflectionTestUtils.getField(command, "persistence"));
        assertThat(payloadMapper).isEqualTo(ReflectionTestUtils.getField(command, "payloadMapper"));
    }

    @Test
    void shouldGenerateCorrectSignature() throws NoSuchAlgorithmException {
        String login = "login";
        String password = "password";
        PrincipalProfilePayload secure = new PrincipalProfilePayload();
        secure.setLogin(login);
        String signature = secure.makeSignatureFor(password);
        assertThat(signature).isNotEmpty();
        assertThat(secure.isPassword(password)).isFalse();
        PrincipalProfilePayload profilePayload =
                PrincipalProfilePayload.builder().login(login).signature(signature).build();
        assertThat(profilePayload.isPassword(password)).isTrue();
        assertThat(profilePayload.isPassword("")).isFalse();
    }

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
    void shouldDoCommand_UpdateProfile() {
        Long id = 700L;
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        doCallRealMethod().when(persistence).save(profile);
        when(profile.getId()).thenReturn(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        when(payloadMapper.toPayload(profile)).thenReturn(payload);
        when(persistence.saveProfile(profile)).thenReturn(Optional.of(profile));
        Context<Optional<PrincipalProfile>> context = command.createContext(profile);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        Optional<PrincipalProfile> doResult = context.getResult().orElseThrow();
        assertThat(doResult.orElseThrow()).isEqualTo(profile);
        assertThat(context.<Object>getUndoParameter()).isEqualTo(payload);
        verify(command).executeDo(context);
        verify(profile).getId();
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(profile);
        verify(payloadMapper).toPayload(profile);
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
        assertThat(context.<Object>getUndoParameter()).isEqualTo(id);
        Optional<PrincipalProfile> doResult = context.getResult().orElseThrow();
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
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).startsWith("Parameter not a  'PrincipalProfile' value:");
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
    void shouldNotDoCommand_FindUpdatedExceptionThrown() {
        Long id = 802L;
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
    void shouldNotDoCommand_SaveCreatedExceptionThrown() {
        doCallRealMethod().when(persistence).save(profile);
        doThrow(RuntimeException.class).when(persistence).saveProfile(profile);
        Context<Optional<PrincipalProfile>> context = command.createContext(profile);

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
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        doCallRealMethod().when(persistence).save(any(PrincipalProfile.class));
        when(persistence.findProfileById(id)).thenReturn(Optional.of(profile));
        when(persistence.toEntity(profile)).thenReturn(profile);
        when(payloadMapper.toPayload(profile)).thenReturn(payload);
        doThrow(RuntimeException.class).when(persistence).saveProfile(any(PrincipalProfile.class));
        Context<Optional<PrincipalProfile>> context = command.createContext(profile);

        assertThrows(RuntimeException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(profile).getId();
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
        verify(persistence).toEntity(profile);
        verify(payloadMapper).toPayload(profile);
        verify(persistence).save(profile);
        verify(persistence).saveProfile(profile);
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
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong input parameter value null");
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Optional<PrincipalProfile>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setUndoParameter("param");
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).startsWith("Parameter not a  'Long' value:[param]");
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

        assertThat(context.isFailed()).isTrue();
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