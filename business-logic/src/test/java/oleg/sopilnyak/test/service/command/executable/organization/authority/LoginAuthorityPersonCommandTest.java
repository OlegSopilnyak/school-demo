package oleg.sopilnyak.test.service.command.executable.organization.authority;

import oleg.sopilnyak.test.school.common.exception.accsess.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.PersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginAuthorityPersonCommandTest {
    @Mock
    PersistenceFacade persistence;
    @Mock
    BusinessMessagePayloadMapper payloadMapper;
    @Spy
    @InjectMocks
    LoginAuthorityPersonCommand command;
    @Mock
    AuthorityPerson entity;
    @Mock
    AuthorityPersonPayload entityPayload;
    @Mock
    PrincipalProfile profile;
    @Mock
    PrincipalProfilePayload profilePayload;

    @Test
    void shouldDoCommand_EntityExists() {
        String username = "login";
        String password = "pass";
        Long id = 330L;
        when(profilePayload.getId()).thenReturn(id);
        when(profilePayload.isPassword(password)).thenReturn(true);
        when(persistence.findPrincipalProfileByLogin(username)).thenReturn(Optional.of(profile));
        when(persistence.findAuthorityPersonByProfileId(id)).thenReturn(Optional.of(entity));
        when(payloadMapper.toPayload(entity)).thenReturn(entityPayload);
        when(payloadMapper.toPayload(profile)).thenReturn(profilePayload);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(username, password));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEqualTo(Optional.of(entityPayload));
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileByLogin(username);
        verify(persistence).findAuthorityPersonByProfileId(id);
    }

    @Test
    void shouldDoCommand_AuthorityPersonNotExists() {
        long id = 331L;
        String username = "login";
        String password = "pass";
        when(profilePayload.getId()).thenReturn(id);
        when(profilePayload.isPassword(password)).thenReturn(true);
        when(persistence.findPrincipalProfileByLogin(username)).thenReturn(Optional.of(profile));
        when(payloadMapper.toPayload(profile)).thenReturn(profilePayload);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(username, password));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEmpty();
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileByLogin(username);
        verify(persistence).findAuthorityPersonByProfileId(id);
    }

    @Test
    void shouldNotDoCommand_PrincipalProfileNotExists() {
        long id = 332L;
        String username = "login";
        String password = "pass";
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(username, password));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Profile with login:'" + username + "', is not found");
        assertThat(context.getResult()).isEmpty();
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileByLogin(username);
        verify(persistence, never()).findAuthorityPersonByProfileId(id);
    }

    @Test
    void shouldNotDoCommand_FindPrincipalProfileThrows() {
        long id = 333L;
        String username = "login";
        String password = "pass";
        String error = "error finding principal profile";
        RuntimeException runtimeException = new RuntimeException(error);
        doThrow(runtimeException).when(persistence).findPrincipalProfileByLogin(username);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(username, password));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(runtimeException);
        assertThat(context.getResult()).isEmpty();
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileByLogin(username);
        verify(persistence, never()).findAuthorityPersonByProfileId(id);
    }

    @Test
    void shouldNotDoCommand_PrincipalProfileWrongPassword() {
        long id = 334L;
        String username = "login";
        String password = "pass";
        when(persistence.findPrincipalProfileByLogin(username)).thenReturn(Optional.of(profile));
        when(payloadMapper.toPayload(profile)).thenReturn(profilePayload);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(username, password));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(SchoolAccessDeniedException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Login authority person command failed for username:" + username);
        assertThat(context.getResult()).isEmpty();
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileByLogin(username);
        verify(persistence, never()).findAuthorityPersonByProfileId(id);
    }

    @Test
    void shouldNotDoCommand_FindAuthorityPersonThrows() {
        long id = 335L;
        String username = "login";
        String password = "pass";
        String error = "error finding authority person";
        RuntimeException runtimeException = new RuntimeException(error);
        when(profilePayload.getId()).thenReturn(id);
        when(profilePayload.isPassword(password)).thenReturn(true);
        when(persistence.findPrincipalProfileByLogin(username)).thenReturn(Optional.of(profile));
        when(payloadMapper.toPayload(profile)).thenReturn(profilePayload);
        doThrow(runtimeException).when(persistence).findAuthorityPersonByProfileId(id);
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(username, password));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(runtimeException);
        assertThat(context.getException().getMessage()).isEqualTo(error);
        assertThat(context.getResult()).isEmpty();
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileByLogin(username);
        verify(persistence).findAuthorityPersonByProfileId(id);
    }

    @Test
    void shouldUndoCommand_NothingToDo() {
        String username = "login";
        String password = "pass";
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(username, password));
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setState(Context.State.DONE);
            commandContext.setUndoParameter(Input.of(entity));
        }
//        context.setState(DONE);
//        context.setUndoParameter(entity);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
    }
}