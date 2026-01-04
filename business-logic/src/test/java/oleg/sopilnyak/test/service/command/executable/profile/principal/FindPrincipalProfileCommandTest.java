package oleg.sopilnyak.test.service.command.executable.profile.principal;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import oleg.sopilnyak.test.school.common.model.PrincipalProfile;
import oleg.sopilnyak.test.school.common.persistence.profile.ProfilePersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.core.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.profile.PrincipalProfileCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.PrincipalProfilePayload;

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
class FindPrincipalProfileCommandTest {
    @Mock
    ProfilePersistenceFacade persistence;
    @Mock
    BusinessMessagePayloadMapper payloadMapper;
    @Spy
    @InjectMocks
    FindPrincipalProfileCommand command;
    @Mock
    PrincipalProfile profile;
    @Mock
    PrincipalProfilePayload payload;
    @Mock
    ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(command, "applicationContext", applicationContext);
        doReturn(command).when(applicationContext).getBean("profilePrincipalFind", PrincipalProfileCommand.class);
    }

    @Test
    void shouldWorkFunctionFindById() {
        reset(applicationContext);
        Long id = 401L;
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);

        command.functionFindById().apply(id);

        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldDoCommand_EntityFound() {
        Long id = 404L;
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        when(persistence.findProfileById(id)).thenReturn(Optional.of(profile));
        when(payloadMapper.toPayload(profile)).thenReturn(payload);
        Context<Optional<PrincipalProfile>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).contains(Optional.of(payload));

        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldDoCommand_EntityNotFound() {
        Long id = 405L;
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        Context<Optional<PrincipalProfile>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElse(Optional.empty())).isEmpty();

        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        long id = 406L;
        Context<Optional<PrincipalProfile>> context = command.createContext(Input.of("" + id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);

        verify(command).executeDo(context);
        verify(persistence, never()).findPrincipalProfileById(anyLong());
    }

    @Test
    void shouldNotDoCommand_FindThrowsException() {
        Long id = 407L;
        doCallRealMethod().when(persistence).findPrincipalProfileById(id);
        doThrow(RuntimeException.class).when(persistence).findProfileById(id);
        Context<Optional<PrincipalProfile>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findPrincipalProfileById(id);
        verify(persistence).findProfileById(id);
    }

    @Test
    void shouldUndoCommand_NothingToDo() {
        Long id = 408L;
        Context<Optional<PrincipalProfile>> context = command.createContext(Input.of(id));
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id));
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
    }
}