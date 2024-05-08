package oleg.sopilnyak.test.service.command.executable.organization.authority;

import oleg.sopilnyak.test.school.common.exception.NotExistAuthorityPersonException;
import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.model.AuthorityPerson;
import oleg.sopilnyak.test.school.common.persistence.organization.AuthorityPersonPersistenceFacade;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrUpdateAuthorityPersonCommandTest {
    @Mock
    AuthorityPersonPersistenceFacade persistence;
    @Spy
    @InjectMocks
    CreateOrUpdateAuthorityPersonCommand command;
    @Mock
    AuthorityPerson entity;

    @Test
    void shouldDoCommand_CreateEntity() {
        Long id = -300L;
        when(entity.getId()).thenReturn(id);
        when(persistence.save(entity)).thenReturn(Optional.of(entity));
        Context<Optional<AuthorityPerson>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isEqualTo(id);
        Optional<AuthorityPerson> doResult = context.getResult().orElseThrow();
        assertThat(doResult.orElseThrow()).isEqualTo(entity);
        verify(command).executeDo(context);
        verify(persistence).save(entity);
    }

    @Test
    void shouldDoCommand_UpdateEntity() {
        Long id = 300L;
        when(entity.getId()).thenReturn(id);
        when(persistence.findAuthorityPersonById(id)).thenReturn(Optional.of(entity));
        when(persistence.toEntity(entity)).thenReturn(entity);
        when(persistence.save(entity)).thenReturn(Optional.of(entity));
        Context<Optional<AuthorityPerson>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isEqualTo(entity);
        Optional<AuthorityPerson> doResult = context.getResult().orElseThrow();
        assertThat(doResult.orElseThrow()).isEqualTo(entity);
        verify(command).executeDo(context);
        verify(entity).getId();
        verify(persistence).findAuthorityPersonById(id);
        verify(persistence).toEntity(entity);
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotDoCommand_EntityNotFound() {
        Long id = 301L;
        when(entity.getId()).thenReturn(id);
        Context<Optional<AuthorityPerson>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistAuthorityPersonException.class);
        assertThat(context.getException().getMessage()).startsWith("AuthorityPerson with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(entity).getId();
        verify(persistence).findAuthorityPersonById(id);
        verify(persistence, never()).toEntity(any());
        verify(persistence, never()).save(any());
    }

    @Test
    void shouldNotDoCommand_FindUpdatedExceptionThrown() {
        Long id = 302L;
        when(entity.getId()).thenReturn(id);
        doThrow(RuntimeException.class).when(persistence).findAuthorityPersonById(id);
        Context<Optional<AuthorityPerson>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(entity).getId();
        verify(persistence).findAuthorityPersonById(id);
        verify(persistence, never()).toEntity(any());
        verify(persistence, never()).save(any());
    }

    @Test
    void shouldNotDoCommand_SaveCreatedExceptionThrown() {
        doThrow(RuntimeException.class).when(persistence).save(entity);
        Context<Optional<AuthorityPerson>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(entity).getId();
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotDoCommand_SaveUpdatedExceptionThrown() {
        Long id = 303L;
        when(entity.getId()).thenReturn(id);
        when(persistence.findAuthorityPersonById(id)).thenReturn(Optional.of(entity));
        when(persistence.toEntity(entity)).thenReturn(entity);
        doThrow(RuntimeException.class).when(persistence).save(entity);
        Context<Optional<AuthorityPerson>> context = command.createContext(entity);

        assertThrows(RuntimeException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(entity).getId();
        verify(persistence).findAuthorityPersonById(id);
        verify(persistence).toEntity(entity);
        verify(persistence, times(2)).save(entity);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<AuthorityPerson>> context = command.createContext("input");

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<AuthorityPerson>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_WrongState() {
        Context<Optional<AuthorityPerson>> context = command.createContext();

        command.doCommand(context);

        assertThat(context.getResult()).isEmpty();
        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeDo(context);
    }

    @Test
    void shouldUndoCommand_CreateEntity() {
        Long id = 304L;
        Context<Optional<AuthorityPerson>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setUndoParameter(id);
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).deleteAuthorityPerson(id);
    }

    @Test
    void shouldUndoCommand_UpdateEntity() {
        Context<Optional<AuthorityPerson>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setUndoParameter(entity);
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotUndoCommand_WrongState() {
        Context<Optional<AuthorityPerson>> context = command.createContext();

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_EmptyParameter() {
        Context<Optional<AuthorityPerson>> context = command.createContext();
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistAuthorityPersonException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong undo parameter :");
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Optional<AuthorityPerson>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setUndoParameter("param");
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistAuthorityPersonException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong undo parameter :");
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_DeleteEntityExceptionThrown() throws NotExistProfileException {
        Long id = 305L;
        Context<Optional<AuthorityPerson>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setUndoParameter(id);
        context.setState(Context.State.DONE);
        doThrow(new RuntimeException()).when(persistence).deleteAuthorityPerson(id);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotUndoCommand_SaveEntityExceptionThrown() {
        Context<Optional<AuthorityPerson>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setUndoParameter(entity);
        context.setState(Context.State.DONE);
        doThrow(new RuntimeException()).when(persistence).save(entity);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(entity);
    }
}