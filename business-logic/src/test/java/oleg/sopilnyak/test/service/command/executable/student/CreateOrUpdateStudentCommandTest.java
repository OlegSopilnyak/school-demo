package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.Disabled;
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
    StudentsPersistenceFacade persistenceFacade;
    @Mock
    Student instance;
    @Spy
    @InjectMocks
    CreateOrUpdateStudentCommand command;

    @Test
    @Disabled
    void shouldExecuteCommand() {

        CommandResult<Optional<Student>> result = command.execute(instance);

        verify(persistenceFacade).save(instance);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Optional.of(mock(Student.class)))).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    @Disabled
    void shouldNotExecuteCommand() {
        RuntimeException cannotExecute = new RuntimeException("Cannot save");
        doThrow(cannotExecute).when(persistenceFacade).save(instance);

        CommandResult<Optional<Student>> result = command.execute(instance);

        verify(persistenceFacade).save(instance);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(Optional.of(mock(Student.class)))).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }

    @Test
    void shouldDoCommand_CreateStudent() {
        Long id = -1L;
        when(instance.getId()).thenReturn(id);
        when(persistenceFacade.save(instance)).thenReturn(Optional.of(instance));
        Context<Optional<Student>> context = command.createContext(instance);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter()).isEqualTo(id);
        assertThat(context.getResult()).isPresent();
        Optional<Student> result = (Optional<Student>) context.getResult().orElseThrow();
        assertThat(result).isPresent();
        assertThat(result).contains(instance);
        verify(command).executeDo(context);
        verify(persistenceFacade).save(instance);
    }

    @Test
    void shouldDoCommand_UpdateStudent() {
        Long id = 110L;
        when(instance.getId()).thenReturn(id);
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(instance));
        when(persistenceFacade.toEntity(instance)).thenReturn(instance);
        when(persistenceFacade.save(instance)).thenReturn(Optional.of(instance));
        Context<Optional<Student>> context = command.createContext(instance);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter()).isEqualTo(instance);
        assertThat(context.getResult()).isPresent();
        Optional<Student> result = (Optional<Student>) context.getResult().orElseThrow();
        assertThat(result).isPresent();
        assertThat(result).contains(instance);
        verify(command).executeDo(context);
        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade).toEntity(instance);
        verify(persistenceFacade).save(instance);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<Student>> context = command.createContext("instance");

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isNotNull().isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
        verify(persistenceFacade, never()).save(instance);
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<Student>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isNotNull().isInstanceOf(NullPointerException.class);
        verify(command).executeDo(context);
        verify(persistenceFacade, never()).save(instance);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = -111L;
        when(instance.getId()).thenReturn(id);
        doThrow(RuntimeException.class).when(persistenceFacade).save(instance);
        Context<Optional<Student>> context = command.createContext(instance);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isNotNull().isInstanceOf(RuntimeException.class);
        verify(command).executeDo(context);
        verify(persistenceFacade).save(instance);
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
        verify(persistenceFacade).deleteStudent(id);
    }

    @Test
    void shouldUndoCommand_RestoreStudent() {
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(instance);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(command).executeUndo(context);
        verify(persistenceFacade).save(instance);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter("instance");

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentNotExistsException.class);
        verify(command).executeUndo(context);
        verify(persistenceFacade, never()).save(instance);
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
        verify(persistenceFacade, never()).save(instance);
    }

    @Test
    void shouldNotUndoCommand_DeleteThrown() {
        Long id = 111L;
        doThrow(RuntimeException.class).when(persistenceFacade).deleteStudent(id);
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(id);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistenceFacade).deleteStudent(id);
    }

    @Test
    void shouldNotUndoCommand_RestoreThrown() {
        doThrow(RuntimeException.class).when(persistenceFacade).save(instance);
        Context<Optional<Student>> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(instance);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(RuntimeException.class);
        verify(command).executeUndo(context);
        verify(persistenceFacade).save(instance);
    }
}