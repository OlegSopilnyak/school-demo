package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.exception.CourseNotExistsException;
import oleg.sopilnyak.test.school.common.exception.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.persistence.students.courses.CoursesPersistenceFacade;
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
class DeleteCourseCommandTest {
    @Mock
    CoursesPersistenceFacade persistenceFacade;
    @Mock
    Course course;
    @Spy
    @InjectMocks
    DeleteCourseCommand command;

    @Test
    @Disabled
    void shouldExecuteCommand() {
        Long id = 100L;

        when(persistenceFacade.findCourseById(id)).thenReturn(Optional.of(course));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade).deleteCourse(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(true)).isFalse();
        assertThat(result.getException()).isNull();
    }

    @Test
    @Disabled
    void shouldNotExecuteCommand_ExceptionThrown() {
        Long id = 101L;
        RuntimeException cannotExecute = new RuntimeException("Cannot delete");
        doThrow(cannotExecute).when(persistenceFacade).deleteCourse(id);
        when(persistenceFacade.findCourseById(id)).thenReturn(Optional.of(course));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade).deleteCourse(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);

    }

    @Test
    @Disabled
    void shouldNotExecuteCommand_NoCourse() {
        Long id = 102L;

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade, never()).deleteCourse(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isEmpty();
        assertThat(result.getException()).isInstanceOf(CourseNotExistsException.class);
    }

    @Test
    @Disabled
    void shouldNotExecuteCommand_CourseNotEmpty() {
        Long id = 103L;
        Student student = mock(Student.class);
        when(course.getStudents()).thenReturn(List.of(student));
        when(persistenceFacade.findCourseById(id)).thenReturn(Optional.of(course));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade, never()).deleteCourse(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isEmpty();
        assertThat(result.getException()).isInstanceOf(CourseWithStudentsException.class);
    }

    @Test
    void shouldDoCommand_CourseFound() {
        Long id = 100L;
        when(persistenceFacade.findCourseById(id)).thenReturn(Optional.of(course));
        when(persistenceFacade.toEntity(course)).thenReturn(course);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter()).isEqualTo(course);
        assertThat(context.getResult()).isPresent();
        Boolean result = (Boolean) context.getResult().orElseThrow();
        assertThat(result).isTrue();
        verify(command).executeDo(context);
        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade).toEntity(any(Course.class));
        verify(persistenceFacade).deleteCourse(id);
    }

    @Test
    void shouldDoCommand_CourseNotFound() {
        Long id = 102L;
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getUndoParameter()).isNull();
        assertThat(context.getException()).isInstanceOf(CourseNotExistsException.class);
        verify(command).executeDo(context);
        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade, never()).deleteCourse(id);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 101L;
        RuntimeException cannotExecute = new RuntimeException("Cannot delete");
        doThrow(cannotExecute).when(persistenceFacade).deleteCourse(id);
        when(persistenceFacade.findCourseById(id)).thenReturn(Optional.of(course));
        when(persistenceFacade.toEntity(course)).thenReturn(course);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade).toEntity(any(Course.class));
        verify(persistenceFacade).deleteCourse(id);
    }

    @Test
    void shouldExecuteUndoCommand() {
        Context<Boolean> context = command.createContext();
        context.setState(DONE);
        context.setUndoParameter(course);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();

        verify(command).executeUndo(context);
        verify(persistenceFacade).save(course);
    }

    @Test
    void shouldNotExecuteUndoCommand_WrongParameterType() {
        Context<Boolean> context = command.createContext();
        context.setState(DONE);
        context.setUndoParameter("course");

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CourseNotExistsException.class);

        verify(command).executeUndo(context);
        verify(persistenceFacade, never()).save(course);
    }

    @Test
    void shouldNotExecuteUndoCommand_NullParameter() {
        Context<Boolean> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);

        verify(command).executeUndo(context);
        verify(persistenceFacade, never()).save(course);
    }

    @Test
    void shouldNotExecuteUndoCommand_ExceptionThrown() {
        Context<Boolean> context = command.createContext();
        RuntimeException cannotExecute = new RuntimeException("Cannot restore");
        doThrow(cannotExecute).when(persistenceFacade).save(course);
        context.setState(DONE);
        context.setUndoParameter(course);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);

        verify(command).executeUndo(context);
        verify(persistenceFacade).save(course);
    }
}