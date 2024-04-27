package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.exception.NotExistCourseException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.students.courses.CoursesPersistenceFacade;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrUpdateCourseCommandTest {
    @Mock
    CoursesPersistenceFacade persistenceFacade;
    @Mock
    Course course;
    @Spy
    @InjectMocks
    CreateOrUpdateCourseCommand command;

    @Test
    @Disabled
    void shouldExecuteCommand() {
        CommandResult<Optional<Course>> result = command.execute(course);

        verify(persistenceFacade).save(course);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Optional.of(mock(Course.class)))).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    @Disabled
    void shouldNotExecuteCommand() {
        RuntimeException cannotExecute = new RuntimeException("Cannot save");
        doThrow(cannotExecute).when(persistenceFacade).save(course);

        CommandResult<Optional<Course>> result = command.execute(course);

        verify(persistenceFacade).save(course);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(Optional.of(mock(Course.class)))).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }

    @Test
    void shouldDoCommand_CreateCourse() {
        Long id = -100L;
        when(course.getId()).thenReturn(id);
        when(persistenceFacade.save(course)).thenReturn(Optional.of(course));

        Context<Optional<Course>> context = command.createContext(course);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter()).isEqualTo(id);
        assertThat(context.getResult()).isPresent();
        Optional<Course> result = (Optional<Course>) context.getResult().orElseThrow();
        assertThat(result).contains(course);
        verify(command).executeDo(context);
        verify(persistenceFacade).save(course);
    }

    @Test
    void shouldDoCommand_UpdateCourse() {
        Long id = 100L;
        when(course.getId()).thenReturn(id);
        when(persistenceFacade.findCourseById(id)).thenReturn(Optional.of(course));
        when(persistenceFacade.toEntity(course)).thenReturn(course);
        when(persistenceFacade.save(course)).thenReturn(Optional.of(course));

        Context<Optional<Course>> context = command.createContext(course);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter()).isEqualTo(course);
        assertThat(context.getResult()).isPresent();
        Optional<Course> result = (Optional<Course>) context.getResult().orElseThrow();
        assertThat(result).contains(course);
        verify(command).executeDo(context);
        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade).toEntity(any(Course.class));
        verify(persistenceFacade).save(course);
    }

    @Test
    void shouldNotDoCommand_WrongParameterType() {
        Context<Optional<Course>> context = command.createContext("course");

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_NullParameter() {
        Context<Optional<Course>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeDo(context);
    }

    @Test
    void shouldNotDoCommand_CreateExceptionThrown() {
        Long id = -102L;
        when(course.getId()).thenReturn(id);
        RuntimeException cannotExecute = new RuntimeException("Cannot create");
        when(persistenceFacade.save(course)).thenThrow(cannotExecute).thenReturn(Optional.of(course));

        Context<Optional<Course>> context = command.createContext(course);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistenceFacade).save(course);
    }

    @Test
    void shouldNotDoCommand_UpdateExceptionThrown() {
        Long id = 102L;
        when(course.getId()).thenReturn(id);
        when(persistenceFacade.findCourseById(id)).thenReturn(Optional.of(course));
        when(persistenceFacade.toEntity(course)).thenReturn(course);
        RuntimeException cannotExecute = new RuntimeException("Cannot update");
        doThrow(cannotExecute).when(persistenceFacade).save(course);

        Context<Optional<Course>> context = command.createContext(course);

        assertThrows(RuntimeException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade).toEntity(any(Course.class));
        verify(persistenceFacade, times(2)).save(course);
    }

    @Test
    void shouldUndoCommand_CreateCourse() {
        Long id = 103L;
        Context<Optional<Course>> context = command.createContext(course);
        context.setState(Context.State.DONE);
        context.setUndoParameter(id);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistenceFacade).deleteCourse(id);
    }

    @Test
    void shouldUndoCommand_UpdateCourse() {
        Context<Optional<Course>> context = command.createContext(course);
        context.setState(Context.State.DONE);
        context.setUndoParameter(course);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistenceFacade).save(course);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Optional<Course>> context = command.createContext(course);
        context.setState(Context.State.DONE);
        context.setUndoParameter("id");

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistCourseException.class);
        assertThat(context.getException().getMessage()).startsWith("Wrong undo parameter :");
        verify(command).executeUndo(context);
        verify(persistenceFacade, never()).deleteCourse(anyLong());
    }

    @Test
    void shouldNotUndoCommand_NullParameter() {
        Context<Optional<Course>> context = command.createContext(course);
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Cannot invoke \"Object.toString()\" because \"parameter\" is null");
        verify(command).executeUndo(context);
        verify(persistenceFacade, never()).deleteCourse(anyLong());
    }

    @Test
    void shouldNotUndoCommand_CreateExceptionThrown() {
        Long id = 104L;
        Context<Optional<Course>> context = command.createContext(course);
        context.setState(Context.State.DONE);
        context.setUndoParameter(id);
        RuntimeException cannotExecute = new RuntimeException("Cannot undo create");
        doThrow(cannotExecute).when(persistenceFacade).deleteCourse(id);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeUndo(context);
        verify(persistenceFacade).deleteCourse(anyLong());
    }

    @Test
    void shouldNotUndoCommand_UpdateExceptionThrown() {
        Context<Optional<Course>> context = command.createContext(course);
        context.setState(Context.State.DONE);
        context.setUndoParameter(course);
        RuntimeException cannotExecute = new RuntimeException("Cannot undo update");
        doThrow(cannotExecute).when(persistenceFacade).save(course);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeUndo(context);
        verify(persistenceFacade).save(course);
    }
}