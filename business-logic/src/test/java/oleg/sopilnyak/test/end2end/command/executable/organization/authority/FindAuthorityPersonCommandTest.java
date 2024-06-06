package oleg.sopilnyak.test.end2end.command.executable.organization.authority;

import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAuthorityPersonCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
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
class FindAuthorityPersonCommandTest {
    @Mock
    AuthorityPersonPersistenceFacade persistence;
    @Spy
    @InjectMocks
    FindAuthorityPersonCommand command;
    @Mock
    AuthorityPerson entity;

    @Test
    void shouldDoCommand_EntityExists() {
        long id = 320L;
        when(persistence.findAuthorityPersonById(id)).thenReturn(Optional.of(entity));
        Context<Optional<AuthorityPerson>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEqualTo(Optional.of(entity));
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findAuthorityPersonById(id);
    }

    @Test
    void shouldDoCommand_EntityNotExists() {
        long id = 321L;
        Context<Optional<AuthorityPerson>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEmpty();
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findAuthorityPersonById(id);
    }

    @Test
    void shouldNotDoCommand_FindThrowsException() {
        long id = 322L;
        Context<Optional<AuthorityPerson>> context = command.createContext(id);
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
        Context<Optional<AuthorityPerson>> context = command.createContext(id);
        context.setState(DONE);
        context.setUndoParameter(entity);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
    }
}