package oleg.sopilnyak.test.service.command.executable.organization.authority;

import static oleg.sopilnyak.test.service.command.type.core.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import oleg.sopilnyak.test.school.common.exception.access.SchoolAccessDeniedException;
import oleg.sopilnyak.test.school.common.exception.profile.ProfileNotFoundException;
import oleg.sopilnyak.test.school.common.model.authentication.AccessCredentials;
import oleg.sopilnyak.test.school.common.security.AuthenticationFacade;
import oleg.sopilnyak.test.service.command.executable.core.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.core.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AccessCredentialsPayload;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LoginAuthorityPersonCommandTest {
    @Mock
    AuthenticationFacade authenticationFacade;
    @Mock
    BusinessMessagePayloadMapper payloadMapper;
    @Spy
    @InjectMocks
    LoginAuthorityPersonCommand command;
    @Mock
    AccessCredentials credentials;
    @Mock
    AccessCredentialsPayload credentialsPayload;
    @Mock
    ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(command, "applicationContext", applicationContext);
        doReturn(command).when(applicationContext).getBean("authorityPersonLogin", AuthorityPersonCommand.class);
    }

    @Test
    void shouldDoCommand_SignedIn() {
        String username = "login";
        String password = "pass";
        String token = "active-token";
        when(payloadMapper.toPayload(credentials)).thenReturn(credentialsPayload);
        doReturn(token).when(credentialsPayload).getToken();
        doReturn(Optional.of(credentials)).when(authenticationFacade).signIn(username, password);
        Context<Optional<AccessCredentials>> context = command.createContext(Input.of(username, password));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEqualTo(Optional.of(credentialsPayload));
        assertThat(context.getUndoParameter()).isEqualTo(Input.of(token));
        verify(command).executeDo(context);
        verify(authenticationFacade).signIn(username, password);
    }

    @Test
    void shouldDoCommand_NotSignedIn_NoUsernameProfile() {
        String username = "login";
        String password = "pass";
        Context<Optional<AccessCredentials>> context = command.createContext(Input.of(username, password));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ProfileNotFoundException.class);
        assertThat(context.getException().getMessage()).isEqualTo(String.format("Profile with login:'%s', is not found", username));
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(authenticationFacade).signIn(username, password);
    }

    @Test
    void shouldNotDoCommand_AccessDenied() {
        String username = "login";
        String password = "pass";
        doThrow(SchoolAccessDeniedException.class).when(authenticationFacade).signIn(username, password);
        Context<Optional<AccessCredentials>> context = command.createContext(Input.of(username, password));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(SchoolAccessDeniedException.class);
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(authenticationFacade).signIn(username, password);
    }

    @Test
    void shouldNotDoCommand_SigningInThrows() {
        String username = "login";
        String password = "pass";
        String error = "error finding principal profile";
        RuntimeException runtimeException = new RuntimeException(error);
        doThrow(runtimeException).when(authenticationFacade).signIn(username, password);
        Context<Optional<AccessCredentials>> context = command.createContext(Input.of(username, password));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(runtimeException);
        assertThat(context.getResult()).isEmpty();
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(authenticationFacade).signIn(username, password);
    }

    @Test
    void shouldUndoCommand_SignOutByToken() {
        String username = "login";
        String password = "pass";
        String token = "active-token";
        Context<Optional<AccessCredentials>> context = command.createContext(Input.of(username, password));
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setState(Context.State.DONE);
            commandContext.setUndoParameter(Input.of(token));
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(authenticationFacade).signOut(token);
    }
}