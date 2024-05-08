package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.exception.NotExistCourseException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.students.courses.CoursesPersistenceFacade;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrUpdateCourseCommandTest {
    @Mock
    CoursesPersistenceFacade persistence;
    @Mock
    Course course;
    @Spy
    @InjectMocks
    CreateOrUpdateCourseCommand command;

    @Test
    void shouldDoCommand_CreateCourse() {
        Long id = -100L;
        when(course.getId()).thenReturn(id);
        when(persistence.save(course)).thenReturn(Optional.of(course));

        Context<Optional<Course>> context = command.createContext(course);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isEqualTo(id);
        assertThat(context.getResult()).isPresent();
        Optional<Course> result = context.getResult().orElseThrow();
        assertThat(result).contains(course);
        verify(command).executeDo(context);
        verify(persistence).save(course);
    }

    @Test
    void shouldDoCommand_UpdateCourse() {
        Long id = 100L;
        when(course.getId()).thenReturn(id);
        when(persistence.findCourseById(id)).thenReturn(Optional.of(course));
        when(persistence.toEntity(course)).thenReturn(course);
        when(persistence.save(course)).thenReturn(Optional.of(course));

        Context<Optional<Course>> context = command.createContext(course);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isEqualTo(course);
        assertThat(context.getResult()).isPresent();
        Optional<Course> result = context.getResult().orElseThrow();
        assertThat(result).contains(course);
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
        verify(persistence).toEntity(any(Course.class));
        verify(persistence).save(course);
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
        when(persistence.save(course)).thenThrow(cannotExecute).thenReturn(Optional.of(course));

        Context<Optional<Course>> context = command.createContext(course);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).save(course);
    }

    @Test
    void shouldNotDoCommand_UpdateExceptionThrown() {
        Long id = 102L;
        when(course.getId()).thenReturn(id);
        when(persistence.findCourseById(id)).thenReturn(Optional.of(course));
        when(persistence.toEntity(course)).thenReturn(course);
        RuntimeException cannotExecute = new RuntimeException("Cannot update");
        doThrow(cannotExecute).when(persistence).save(course);

        Context<Optional<Course>> context = command.createContext(course);

        assertThrows(RuntimeException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
        verify(persistence).toEntity(any(Course.class));
        verify(persistence, times(2)).save(course);
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
        verify(persistence).deleteCourse(id);
    }

    @Test
    void shouldUndoCommand_UpdateCourse() {
        Context<Optional<Course>> context = command.createContext(course);
        context.setState(Context.State.DONE);
        context.setUndoParameter(course);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).save(course);
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
        verify(persistence, never()).deleteCourse(anyLong());
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
        verify(persistence, never()).deleteCourse(anyLong());
    }

    @Test
    void shouldNotUndoCommand_CreateExceptionThrown() {
        Long id = 104L;
        Context<Optional<Course>> context = command.createContext(course);
        context.setState(Context.State.DONE);
        context.setUndoParameter(id);
        RuntimeException cannotExecute = new RuntimeException("Cannot undo create");
        doThrow(cannotExecute).when(persistence).deleteCourse(id);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeUndo(context);
        verify(persistence).deleteCourse(anyLong());
    }

    @Test
    void shouldNotUndoCommand_UpdateExceptionThrown() {
        Context<Optional<Course>> context = command.createContext(course);
        context.setState(Context.State.DONE);
        context.setUndoParameter(course);
        RuntimeException cannotExecute = new RuntimeException("Cannot undo update");
        doThrow(cannotExecute).when(persistence).save(course);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeUndo(context);
        verify(persistence).save(course);
    }
}