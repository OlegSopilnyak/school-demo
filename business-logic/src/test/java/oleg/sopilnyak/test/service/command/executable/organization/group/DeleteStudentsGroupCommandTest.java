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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteStudentsGroupCommandTest {
    @Mock
    StudentsGroupPersistenceFacade persistence;
    @Spy
    @InjectMocks
    DeleteStudentsGroupCommand command;
    @Mock
    StudentsGroup entity;

    @Test
    void shouldDoCommand_EntityExists() {
        long id = 514L;
        when(persistence.findStudentsGroupById(id)).thenReturn(Optional.of(entity));
        when(persistence.toEntity(entity)).thenReturn(entity);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).contains(true);
        assertThat(context.getUndoParameter()).isEqualTo(entity);
        verify(command).executeDo(context);
        verify(persistence).findStudentsGroupById(id);
        verify(persistence).toEntity(entity);
        verify(persistence).deleteStudentsGroup(id);
    }

    @Test
    void shouldNotDoCommand_EntityNotExists() {
        long id = 515L;
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistStudentsGroupException.class);
        assertThat(context.getException().getMessage()).startsWith("Students Group with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence).findStudentsGroupById(id);
        verify(persistence, never()).toEntity(entity);
        verify(persistence, never()).deleteStudentsGroup(id);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Boolean> context = command.createContext("id");

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
        verify(persistence, never()).findStudentsGroupById(anyLong());
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Boolean> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistStudentsGroupException.class);
        assertThat(context.getException().getMessage()).startsWith("Students Group with ID:").endsWith(" is not exists.");
        verify(command).executeDo(context);
        verify(persistence, never()).findStudentsGroupById(anyLong());
    }

    @Test
    void shouldNotDoCommand_DeleteExceptionThrown() throws NotExistProfileException {
        long id = 516L;
        when(persistence.findStudentsGroupById(id)).thenReturn(Optional.of(entity));
        when(persistence.toEntity(entity)).thenReturn(entity);
        doThrow(new UnsupportedOperationException()).when(persistence).deleteStudentsGroup(id);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(UnsupportedOperationException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentsGroupById(id);
        verify(persistence).deleteStudentsGroup(id);
    }

    @Test
    void shouldUndoCommand_UndoParameterIsCorrect() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(entity);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistence).save(entity);
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
        verify(persistence, never()).save(entity);
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
        verify(persistence, never()).save(entity);
    }

    @Test
    void shouldNotUndoCommand_ExceptionThrown() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(entity);
        doThrow(new UnsupportedOperationException()).when(persistence).save(entity);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(UnsupportedOperationException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(entity);
    }
}