package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.exception.StudentWithCoursesException;
import oleg.sopilnyak.test.school.common.persistence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
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

import java.util.List;
import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteStudentCommandTest {
    @Mock
    StudentsPersistenceFacade persistenceFacade;
    @Mock
    Student instance;
    @Spy
    @InjectMocks
    DeleteStudentCommand command;

    @Test
    @Disabled
    void shouldExecuteCommand() {
        Long id = 110L;
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(instance));
        when(persistenceFacade.deleteStudent(id)).thenReturn(true);

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade).deleteStudent(id);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(false)).isTrue();
        assertThat(result.getException()).isNull();
    }

    @Test
    @Disabled
    void shouldNotExecuteCommand() {
        Long id = 111L;
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(instance));
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).deleteStudent(id);

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade).deleteStudent(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(true)).isFalse();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }

    @Test
    @Disabled
    void shouldNotExecuteCommand_NoStudent() {
        Long id = 112L;

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findStudentById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(true)).isFalse();
        assertThat(result.getException()).isInstanceOf(NotExistStudentException.class);
    }

    @Test
    @Disabled
    void shouldNotExecuteCommand_HasCourses() {
        Long id = 113L;
        when(instance.getCourses()).thenReturn(List.of(mock(Course.class)));
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(instance));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findStudentById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(true)).isFalse();
        assertThat(result.getException()).isInstanceOf(StudentWithCoursesException.class);
    }

    @Test
    void shouldDoCommand_StudentFound() {
        Long id = 110L;
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(instance));
        when(persistenceFacade.deleteStudent(id)).thenReturn(true);
        when(persistenceFacade.toEntity(instance)).thenReturn(instance);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Boolean result = (Boolean) context.getResult().orElseThrow();
        assertThat(result).isTrue();
        verify(command).executeDo(context);
        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade).toEntity(instance);
        verify(persistenceFacade).deleteStudent(id);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 111L;
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(instance));
        when(persistenceFacade.toEntity(instance)).thenReturn(instance);
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).deleteStudent(id);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade).toEntity(instance);
        verify(persistenceFacade).deleteStudent(id);
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
        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade, never()).toEntity(instance);
        verify(persistenceFacade, never()).deleteStudent(id);
    }

    @Test
    void shouldNotDoCommand_StudentHasCourses() {
        Long id = 113L;
        when(instance.getCourses()).thenReturn(List.of(mock(Course.class)));
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(instance));
        when(persistenceFacade.toEntity(instance)).thenReturn(instance);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentWithCoursesException.class);
        verify(command).executeDo(context);
        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade).toEntity(instance);
        verify(persistenceFacade, never()).deleteStudent(id);
    }

    @Test
    void shouldUndoCommand_RestoreStudent() {
        Context<Boolean> context = command.createContext();
        context.setState(DONE);
        context.setUndoParameter(instance);
        when(persistenceFacade.save(instance)).thenReturn(Optional.of(instance));

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistenceFacade).save(instance);
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
        verify(persistenceFacade, never()).save(instance);
    }

    @Test
    void shouldNotUndoCommand_NullParameter() {
        Context<Boolean> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeUndo(context);
        verify(persistenceFacade, never()).save(instance);
    }
}