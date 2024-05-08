package oleg.sopilnyak.test.service.command.executable.organization.group;

import oleg.sopilnyak.test.school.common.exception.NotExistProfileException;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentsGroupException;
import oleg.sopilnyak.test.school.common.model.StudentsGroup;
import oleg.sopilnyak.test.school.common.persistence.organization.StudentsGroupPersistenceFacade;
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
class CreateOrUpdateStudentsGroupCommandTest {
    @Mock
    StudentsGroupPersistenceFacade persistence;
    @Spy
    @InjectMocks
    CreateOrUpdateStudentsGroupCommand command;
    @Mock
    StudentsGroup entity;

    @Test
    void shouldDoCommand_CreateEntity() {
        Long id = -500L;
        when(entity.getId()).thenReturn(id);
        when(persistence.save(entity)).thenReturn(Optional.of(entity));
        Context<Optional<StudentsGroup>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isEqualTo(id);
        Optional<StudentsGroup> doResult = context.getResult().orElseThrow();
        assertThat(doResult.orElseThrow()).isEqualTo(entity);
        verify(command).executeDo(context);
        verify(persistence).save(entity);
    }

    @Test
    void shouldDoCommand_UpdateEntity() {
        Long id = 500L;
        when(entity.getId()).thenReturn(id);
        when(persistence.findStudentsGroupById(id)).thenReturn(Optional.of(entity));
        when(persistence.toEntity(entity)).thenReturn(entity);
        when(persistence.save(entity)).thenReturn(Optional.of(entity));
        Context<Optional<StudentsGroup>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isEqualTo(entity);
        Optional<StudentsGroup> doResult = context.getResult().orElseThrow();
        assertThat(doResult.orElseThrow()).isEqualTo(entity);
        verify(command).executeDo(context);
        verify(entity).getId();
        verify(persistence).findStudentsGroupById(id);
        verify(persistence).toEntity(entity);
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotDoCommand_EntityNotFound() {
        Long id = 501L;
        when(entity.getId()).thenReturn(id);
        Context<Optional<StudentsGroup>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistStudentsGroupException.class);
        assertThat(context.getException().getMessage()).startsWith("Students Group with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(entity).getId();
        verify(persistence).findStudentsGroupById(id);
        verify(persistence, never()).toEntity(any());
        verify(persistence, never()).save(any());
    }

    @Test
    void shouldNotDoCommand_FindUpdatedExceptionThrown() {
        Long id = 502L;
        when(entity.getId()).thenReturn(id);
        doThrow(RuntimeException.class).when(persistence).findStudentsGroupById(id);
        Context<Optional<StudentsGroup>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(entity).getId();
        verify(persistence).findStudentsGroupById(id);
        verify(persistence, never()).toEntity(any());
        verify(persistence, never()).save(any());
    }

    @Test
    void shouldNotDoCommand_SaveCreatedExceptionThrown() {
        doThrow(RuntimeException.class).when(persistence).save(entity);
        Context<Optional<StudentsGroup>> context = command.createContext(entity);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(entity).getId();
        verify(persistence).save(entity);
    }

    @Test
    void shouldNotDoCommand_SaveUpdatedExceptionThrown() {
        Long id = 503L;
        when(entity.getId()).thenReturn(id);
        when(persistence.findStudentsGroupById(id)).thenReturn(Optional.of(entity));
        when(persistence.toEntity(entity)).thenReturn(entity);
        doThrow(RuntimeException.class).when(persistence).save(entity);
        Context<Optional<StudentsGroup>> context = command.createContext(entity);

        assertThrows(RuntimeException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(entity).getId();
        verify(persistence).findStudentsGroupById(id);
        verify(persistence).toEntity(entity);
        verify(persistence, times(2)).save(entity);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<StudentsGroup>> context = command.createContext("input");

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<StudentsGroup>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_WrongState() {
        Context<Optional<StudentsGroup>> context = command.createContext();

        command.doCommand(context);

        assertThat(context.getResult()).isEmpty();
        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeDo(context);
    }

    @Test
    void shouldUndoCommand_CreateEntity() {
        Long id = 504L;
        Context<Optional<StudentsGroup>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setUndoParameter(id);
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).deleteStudentsGroup(id);
    }

    @Test
    void shouldUndoCommand_UpdateEntity() {
        Context<Optional<StudentsGroup>> context = command.createContext();
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
        Context<Optional<StudentsGroup>> context = command.createContext();

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        verify(command, never()).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_EmptyParameter() {
        Context<Optional<StudentsGroup>> context = command.createContext();
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistStudentsGroupException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong undo parameter :");
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Optional<StudentsGroup>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setUndoParameter("param");
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistStudentsGroupException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong undo parameter :");
        verify(command).executeUndo(context);
    }

    @Test
    void shouldNotUndoCommand_DeleteEntityExceptionThrown() throws NotExistProfileException {
        Long id = 505L;
        Context<Optional<StudentsGroup>> context = command.createContext();
        context.setState(Context.State.WORK);
        context.setUndoParameter(id);
        context.setState(Context.State.DONE);
        doThrow(new RuntimeException()).when(persistence).deleteStudentsGroup(id);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).deleteStudentsGroup(id);
    }

    @Test
    void shouldNotUndoCommand_SaveEntityExceptionThrown() {
        Context<Optional<StudentsGroup>> context = command.createContext();
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