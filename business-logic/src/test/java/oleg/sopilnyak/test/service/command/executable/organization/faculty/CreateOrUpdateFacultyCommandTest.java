package oleg.sopilnyak.test.service.command.executable.organization.faculty;

import oleg.sopilnyak.test.school.common.exception.NotExistFacultyException;
import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.model.Faculty;
import oleg.sopilnyak.test.school.common.persistence.organization.FacultyPersistenceFacade;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrUpdateFacultyCommandTest {
    @Mock
    FacultyPersistenceFacade persistence;
    @Spy
    @InjectMocks
    CreateOrUpdateFacultyCommand command;
    @Mock
    Faculty entity;

    @Test
    void shouldDoCommand_CreateEntity() {
        Long id = -400L;
        when(entity.getId()).thenReturn(id);
        when(persistence.save(entity)).thenReturn(Optional.of(entity));
        Context<Optional<Faculty>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isEqualTo(id);
        Optional<Faculty> doResult = context.getResult().orElseThrow();
        assertThat(doResult.orElseThrow()).isEqualTo(entity);
        verify(command).executeDo(context);
        verify(persistence).save(entity);
    }

    @Test
    void shouldDoCommand_UpdateEntity() {
        Long id = 400L;
        when(entity.getId()).thenReturn(id);
        when(persistence.findFacultyById(id)).thenReturn(Optional.of(entity));
        when(persistence.toEntity(entity)).thenReturn(entity);
        when(persistence.save(entity)).thenReturn(Optional.of(entity));
        Context<Optional<Faculty>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isEqualTo(entity);
        Optional<Faculty> doResult = context.getResult().orElseThrow();
        assertThat(doResult.orElseThrow()).isEqualTo(entity);
        verify(command).executeDo(context);
        verify(entity).getId();
        verify(persistence).findFacultyById(id);
        verify(persistence).toEntity(entity);
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotDoCommand_EntityNotFound() {
        Long id = 401L;
        when(entity.getId()).thenReturn(id);
        Context<Optional<Faculty>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistFacultyException.class);
        assertThat(context.getException().getMessage()).startsWith("Faculty with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(entity).getId();
        verify(persistence).findFacultyById(id);
        verify(persistence, never()).toEntity(any());
        verify(persistence, never()).save(any());
    }

    @Test
    void shouldNotDoCommand_FindUpdatedExceptionThrown() {
        Long id = 402L;
        when(entity.getId()).thenReturn(id);
        doThrow(RuntimeException.class).when(persistence).findFacultyById(id);
        Context<Optional<Faculty>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(entity).getId();
        verify(persistence).findFacultyById(id);
        verify(persistence, never()).toEntity(any());
        verify(persistence, never()).save(any());
    }

    @Test
    void shouldNotDoCommand_SaveCreatedExceptionThrown() {
        doThrow(RuntimeException.class).when(persistence).save(entity);
        Context<Optional<Faculty>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(entity).getId();
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotDoCommand_SaveUpdatedExceptionThrown() {
        Long id = 403L;
        when(entity.getId()).thenReturn(id);
        when(persistence.findFacultyById(id)).thenReturn(Optional.of(entity));
        when(persistence.toEntity(entity)).thenReturn(entity);
        doThrow(RuntimeException.class).when(persistence).save(entity);
        Context<Optional<Faculty>> context = command.createContext(entity);

        assertThrows(RuntimeException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(entity).getId();
        verify(persistence).findFacultyById(id);
        verify(persistence).toEntity(entity);
        verify(persistence, times(2)).save(entity);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<Faculty>> context = command.createContext("input");

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<Faculty>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_WrongState() {
        Context<Optional<Faculty>> context = command.createContext();

        command.doCommand(context);

        assertThat(context.getResult()).isEmpty();
        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeDo(context);
    }

    @Test
    void shouldUndoCommand_CreateEntity() {
        Long id = 404L;
        Context<Optional<Faculty>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setUndoParameter(id);
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).deleteFaculty(id);
    }

    @Test
    void shouldUndoCommand_UpdateEntity() {
        Context<Optional<Faculty>> context = command.createContext();
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
        Context<Optional<Faculty>> context = command.createContext();

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_EmptyParameter() {
        Context<Optional<Faculty>> context = command.createContext();
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistFacultyException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong undo parameter :");
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Optional<Faculty>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setUndoParameter("param");
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistFacultyException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong undo parameter :");
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_DeleteEntityExceptionThrown() throws NotExistProfileException {
        Long id = 405L;
        Context<Optional<Faculty>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setUndoParameter(id);
        context.setState(Context.State.DONE);
        doThrow(new RuntimeException()).when(persistence).deleteFaculty(id);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).deleteFaculty(id);
    }

    @Test
    void shouldNotUndoCommand_SaveEntityExceptionThrown() {
        Context<Optional<Faculty>> context = command.createContext();
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