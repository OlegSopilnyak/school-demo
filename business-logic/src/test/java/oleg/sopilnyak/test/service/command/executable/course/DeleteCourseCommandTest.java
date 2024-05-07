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

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteCourseCommandTest {
    @Mock
    CoursesPersistenceFacade persistence;
    @Mock
    Course course;
    @Spy
    @InjectMocks
    DeleteCourseCommand command;

    @Test
    void shouldDoCommand_CourseFound() {
        Long id = 100L;
        when(persistence.findCourseById(id)).thenReturn(Optional.of(course));
        when(persistence.toEntity(course)).thenReturn(course);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter()).isEqualTo(course);
        assertThat(context.getResult()).isPresent();
        Boolean result = context.getResult().orElseThrow();
        assertThat(result).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
        verify(persistence).toEntity(any(Course.class));
        verify(persistence).deleteCourse(id);
    }

    @Test
    void shouldNotDoCommand_CourseNotFound() {
        Long id = 102L;
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getUndoParameter()).isNull();
        assertThat(context.getException()).isInstanceOf(NotExistCourseException.class);
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
        verify(persistence, never()).deleteCourse(id);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 101L;
        RuntimeException cannotExecute = new RuntimeException("Cannot delete");
        doThrow(cannotExecute).when(persistence).deleteCourse(id);
        when(persistence.findCourseById(id)).thenReturn(Optional.of(course));
        when(persistence.toEntity(course)).thenReturn(course);
        Context<Boolean> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
        verify(persistence).toEntity(any(Course.class));
        verify(persistence).deleteCourse(id);
    }

    @Test
    void shouldUndoCommand_CourseFound() {
        Context<Boolean> context = command.createContext();
        context.setState(DONE);
        context.setUndoParameter(course);
        when(persistence.save(course)).thenReturn(Optional.of(course));

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistence).save(course);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Boolean> context = command.createContext();
        context.setState(DONE);
        context.setUndoParameter("course");

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistCourseException.class);
        verify(command).executeUndo(context);
        verify(persistence, never()).save(course);
    }

    @Test
    void shouldNotUndoCommand_NullParameter() {
        Context<Boolean> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeUndo(context);
        verify(persistence, never()).save(course);
    }

    @Test
    void shouldNotUndoCommand_ExceptionThrown() {
        Context<Boolean> context = command.createContext();
        RuntimeException cannotExecute = new RuntimeException("Cannot restore");
        doThrow(cannotExecute).when(persistence).save(course);
        context.setState(DONE);
        context.setUndoParameter(course);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);

        verify(command).executeUndo(context);
        verify(persistence).save(course);
    }
}