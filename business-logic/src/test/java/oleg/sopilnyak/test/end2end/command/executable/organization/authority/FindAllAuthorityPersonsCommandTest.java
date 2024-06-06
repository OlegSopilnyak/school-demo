package oleg.sopilnyak.test.end2end.command.executable.organization.authority;

import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.organization.authority.FindAllAuthorityPersonsCommand;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindAllAuthorityPersonsCommandTest {
    @Mock
    AuthorityPersonPersistenceFacade persistence;
    @Spy
    @InjectMocks
    FindAllAuthorityPersonsCommand command;
    @Mock
    AuthorityPerson entity;

    @Test
    void shouldDoCommand_EntityExists() {
        when(persistence.findAllAuthorityPersons()).thenReturn(Set.of(entity));
        Context<Set<AuthorityPerson>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEqualTo(Set.of(entity));
        assertThat(context.<Object>getUndoParameter()).isNull();
        verify(command).executeDo(context);
        verify(persistence).findAllAuthorityPersons();
    }

    @Test
    void shouldDoCommand_EntityNotExists() {
        Context<Set<AuthorityPerson>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult().orElseThrow()).isEqualTo(Set.of());
        assertThat(context.<Object>getUndoParameter()).isNull();
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
        context.setState(DONE);
        context.setUndoParameter(entity);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
    }
}