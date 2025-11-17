package oleg.sopilnyak.test.service.command.executable.organization.authority;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;

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
class FindAuthorityPersonCommandTest {
    @Mock
    AuthorityPersonPersistenceFacade persistence;
    @Mock
    BusinessMessagePayloadMapper payloadMapper;
    @Spy
    @InjectMocks
    FindAuthorityPersonCommand command;
    @Mock
    AuthorityPerson entity;
    @Mock
    AuthorityPersonPayload payload;
    @Mock
    ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(command, "applicationContext", applicationContext);
        doReturn(command).when(applicationContext).getBean("authorityPersonFind", AuthorityPersonCommand.class);
    }

    @Test
    void shouldDoCommand_EntityExists() {
        long id = 320L;
        when(persistence.findAuthorityPersonById(id)).thenReturn(Optional.of(entity));
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(id));
        when(payloadMapper.toPayload(entity)).thenReturn(payload);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEqualTo(Optional.of(payload));
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findAuthorityPersonById(id);
    }

    @Test
    void shouldDoCommand_EntityNotExists() {
        long id = 321L;
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEmpty();
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findAuthorityPersonById(id);
    }

    @Test
    void shouldNotDoCommand_FindThrowsException() {
        long id = 322L;
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(id));
        doThrow(RuntimeException.class).when(persistence).findAuthorityPersonById(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findAuthorityPersonById(id);
    }

    @Test
    void shouldUndoCommand_NothingToDo() {
        long id = 323L;
        Context<Optional<AuthorityPerson>> context = command.createContext(Input.of(id));
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(entity));
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
    }
}