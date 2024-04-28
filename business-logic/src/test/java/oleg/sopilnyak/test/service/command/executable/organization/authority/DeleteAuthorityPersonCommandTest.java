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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteAuthorityPersonCommandTest {
    @Mock
    AuthorityPersonPersistenceFacade persistence;
    @Spy
    @InjectMocks
    DeleteAuthorityPersonCommand command;
    @Mock
    AuthorityPerson person;

    @Test
    void shouldDoCommand_EntityExists() {
        long id = 314L;
        when(persistence.findAuthorityPersonById(id)).thenReturn(Optional.of(person));
        when(persistence.toEntity(person)).thenReturn(person);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).contains(true);
        assertThat(context.getUndoParameter()).isEqualTo(person);
        verify(command).executeDo(context);
        verify(persistence).findAuthorityPersonById(id);
        verify(persistence).toEntity(person);
        verify(persistence).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotDoCommand_EntityNotExists() {
        long id = 315L;
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistAuthorityPersonException.class);
        assertThat(context.getException().getMessage()).startsWith("AuthorityPerson with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findAuthorityPersonById(id);
        verify(persistence, never()).toEntity(person);
        verify(persistence, never()).deleteAuthorityPerson(id);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Boolean> context = command.createContext("id");

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
        verify(persistence, never()).findAuthorityPersonById(anyLong());
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Boolean> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistAuthorityPersonException.class);
        assertThat(context.getException().getMessage()).startsWith("AuthorityPerson with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence, never()).findAuthorityPersonById(anyLong());
    }

    @Test
    void shouldNotDoCommand_DeleteExceptionThrown() throws NotExistProfileException {
        long id = 316L;
        when(persistence.findAuthorityPersonById(id)).thenReturn(Optional.of(person));
        when(persistence.toEntity(person)).thenReturn(person);
        doThrow(new UnsupportedOperationException()).when(persistence).deleteAuthorityPerson(id);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(UnsupportedOperationException.class);
        verify(command).executeDo(context);
        verify(persistence).findAuthorityPersonById(id);
        verify(persistence).deleteAuthorityPerson(id);
    }

    @Test
    void shouldUndoCommand_UndoParameterIsCorrect() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(person);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistence).save(person);
    }

    @Test
    void shouldUndoCommand_UndoParameterWrongType() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter("person");

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistence, never()).save(person);
    }

    @Test
    void shouldUndoCommand_UndoParameterIsNull() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(null);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistence, never()).save(person);
    }

    @Test
    void shouldNotUndoCommand_ExceptionThrown() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(person);
        doThrow(new UnsupportedOperationException()).when(persistence).save(person);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(UnsupportedOperationException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(person);
    }
}