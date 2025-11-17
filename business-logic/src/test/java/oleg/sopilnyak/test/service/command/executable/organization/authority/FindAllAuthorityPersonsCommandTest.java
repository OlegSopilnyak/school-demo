package oleg.sopilnyak.test.service.command.executable.organization.authority;

import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.context.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.command.type.organization.AuthorityPersonCommand;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.AuthorityPersonPayload;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindAllAuthorityPersonsCommandTest {
    @Mock
    AuthorityPersonPersistenceFacade persistence;
    @Mock
    BusinessMessagePayloadMapper payloadMapper;
    @Spy
    @InjectMocks
    FindAllAuthorityPersonsCommand command;
    @Mock
    AuthorityPerson entity;
    @Mock
    AuthorityPersonPayload payload;
    @Mock
    ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(command, "applicationContext", applicationContext);
        doReturn(command).when(applicationContext).getBean("authorityPersonFindAll", AuthorityPersonCommand.class);
    }

    @Test
    void shouldDoCommand_EntityExists() {
        when(persistence.findAllAuthorityPersons()).thenReturn(Set.of(entity));
        Context<Set<AuthorityPerson>> context = command.createContext(null);
        when(payloadMapper.toPayload(entity)).thenReturn(payload);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEqualTo(Set.of(payload));
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findAllAuthorityPersons();
    }

    @Test
    void shouldDoCommand_EntityNotExists() {
        Context<Set<AuthorityPerson>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEqualTo(Set.of());
        assertThat(context.getUndoParameter().isEmpty()).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findAllAuthorityPersons();
    }

    @Test
    void shouldNotDoCommand_FindThrowsException() {
        doThrow(RuntimeException.class).when(persistence).findAllAuthorityPersons();
        Context<Set<AuthorityPerson>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistence).findAllAuthorityPersons();
    }

    @Test
    void shouldUndoCommand_NothingToDo() {
        Context<Set<AuthorityPerson>> context = command.createContext(null);
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