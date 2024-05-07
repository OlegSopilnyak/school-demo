package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrUpdateStudentCommandTest {
    @Mock
    StudentsPersistenceFacade persistence;
    @Mock
    Student instance;
    @Spy
    @InjectMocks
    CreateOrUpdateStudentCommand command;

    @Test
    void shouldDoCommand_CreateStudent() {
        Long id = -1L;
        when(instance.getId()).thenReturn(id);
        when(persistence.save(instance)).thenReturn(Optional.of(instance));
        Context<Optional<Student>> context = command.createContext(instance);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter()).isEqualTo(id);
        assertThat(context.getResult()).isPresent();
        Optional<Student> result = context.getResult().orElseThrow();
        assertThat(result).isPresent().contains(instance);
        verify(command).executeDo(context);
        verify(persistence).save(instance);
    }

    @Test
    void shouldDoCommand_UpdateStudent() {
        Long id = 110L;
        when(instance.getId()).thenReturn(id);
        when(persistence.findStudentById(id)).thenReturn(Optional.of(instance));
        when(persistence.toEntity(instance)).thenReturn(instance);
        when(persistence.save(instance)).thenReturn(Optional.of(instance));
        Context<Optional<Student>> context = command.createContext(instance);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter()).isEqualTo(instance);
        assertThat(context.getResult()).isPresent();
        Optional<Student> result = context.getResult().orElseThrow();
        assertThat(result).isPresent().contains(instance);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence).toEntity(instance);
        verify(persistence).save(instance);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<Student>> context = command.createContext("instance");

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isNotNull().isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
        verify(persistence, never()).save(instance);
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<Student>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isNotNull().isInstanceOf(NullPointerException.class);
        verify(command).executeDo(context);
        verify(persistence, never()).save(instance);
    }

    @Test
    void shouldNotDoCommand_CreateExceptionThrown() {
        Long id = -111L;
        when(instance.getId()).thenReturn(id);
        RuntimeException cannotExecute = new RuntimeException("Cannot create");
        doThrow(cannotExecute).when(persistence).save(instance);
        Context<Optional<Student>> context = command.createContext(instance);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).save(instance);
    }

    @Test
    void shouldNotDoCommand_UpdateExceptionThrown() {
        Long id = 111L;
        when(instance.getId()).thenReturn(id);
        when(persistence.findStudentById(id)).thenReturn(Optional.of(instance));
        when(persistence.toEntity(instance)).thenReturn(instance);
        RuntimeException cannotExecute = new RuntimeException("Cannot update");
        when(persistence.save(instance)).thenThrow(cannotExecute).thenReturn(Optional.of(instance));
        Context<Optional<Student>> context = command.createContext(instance);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getUndoParameter()).isEqualTo(instance);
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence).toEntity(instance);
        verify(persistence, times(2)).save(instance);
    }

    @Test
    void shouldUndoCommand_DeleteStudent() {
        Long id = 111L;
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(id);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).deleteStudent(id);
    }

    @Test
    void shouldUndoCommand_RestoreStudent() {
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(instance);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).save(instance);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter("instance");

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistStudentException.class);
        verify(command).executeUndo(context);
        verify(persistence, never()).save(instance);
    }

    @Test
    void shouldNotUndoCommand_NullParameter() {
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(null);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeUndo(context);
        verify(persistence, never()).save(instance);
    }

    @Test
    void shouldNotUndoCommand_DeleteThrown() {
        Long id = 111L;
        doThrow(RuntimeException.class).when(persistence).deleteStudent(id);
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(id);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).deleteStudent(id);
    }

    @Test
    void shouldNotUndoCommand_RestoreThrown() {
        doThrow(RuntimeException.class).when(persistence).save(instance);
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(instance);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistence).save(instance);
    }
}