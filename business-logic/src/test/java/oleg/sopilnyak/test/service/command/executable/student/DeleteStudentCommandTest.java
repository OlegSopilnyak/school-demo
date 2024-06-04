package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.exception.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.persistence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteStudentCommandTest {
    @Mock
    StudentsPersistenceFacade persistence;
    @Mock
    Student instance;
    @Spy
    @InjectMocks
    DeleteStudentCommand command;

    @Test
    void shouldDoCommand_StudentFound() {
        Long id = 110L;
        when(persistence.findStudentById(id)).thenReturn(Optional.of(instance));
        when(persistence.deleteStudent(id)).thenReturn(true);
//        when(persistence.toEntity(instance)).thenReturn(instance);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Boolean result = context.getResult().orElseThrow();
        assertThat(result).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
//        verify(persistence).toEntity(instance);
        verify(persistence).deleteStudent(id);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 111L;
        when(persistence.findStudentById(id)).thenReturn(Optional.of(instance));
//        when(persistence.toEntity(instance)).thenReturn(instance);
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistence).deleteStudent(id);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
//        verify(persistence).toEntity(instance);
        verify(persistence).deleteStudent(id);
    }

    @Test
    void shouldNotDoCommand_StudentNotFound() {
        Long id = 112L;
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistStudentException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
//        verify(persistence, never()).toEntity(instance);
        verify(persistence, never()).deleteStudent(id);
    }

    @Test
    void shouldNotDoCommand_StudentHasCourses() {
        Long id = 113L;
        when(instance.getCourses()).thenReturn(List.of(mock(Course.class)));
        when(persistence.findStudentById(id)).thenReturn(Optional.of(instance));
//        when(persistence.toEntity(instance)).thenReturn(instance);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentWithCoursesException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
//        verify(persistence).toEntity(instance);
        verify(persistence, never()).deleteStudent(id);
    }

    @Test
    void shouldUndoCommand_RestoreStudent() {
        Context<Boolean> context = command.createContext();
        context.setState(DONE);
        context.setUndoParameter(instance);
        when(persistence.save(instance)).thenReturn(Optional.of(instance));

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistence).save(instance);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Boolean> context = command.createContext();
        context.setState(DONE);
        context.setUndoParameter("instance");

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistStudentException.class);
        verify(command).executeUndo(context);
        verify(persistence, never()).save(instance);
    }

    @Test
    void shouldNotUndoCommand_NullParameter() {
        Context<Boolean> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeUndo(context);
        verify(persistence, never()).save(instance);
    }
}