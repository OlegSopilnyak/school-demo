package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.exception.CourseNotExistsException;
import oleg.sopilnyak.test.school.common.exception.StudentNotExistsException;
import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
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

import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnRegisterStudentFromCourseCommandTest {
    @Mock
    PersistenceFacade persistenceFacade;
    @Mock
    Course course;
    @Mock
    Student student;
    @Spy
    @InjectMocks
    UnRegisterStudentFromCourseCommand command;

    @Test
    @Disabled
    void shouldExecuteCommand() {
        Long id = 130L;
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(student));
        when(persistenceFacade.findCourseById(id)).thenReturn(Optional.of(course));
        when(persistenceFacade.unLink(student, course)).thenReturn(true);

        CommandResult<Boolean> result = command.execute(new Long[]{id, id});

        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade).unLink(student, course);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(false)).isTrue();
        assertThat(result.getException()).isNull();
    }

    @Test
    @Disabled
    void shouldNotExecuteCommand_ExceptionThrown() {
        Long id = 131L;
        RuntimeException cannotExecute = new RuntimeException("Cannot un-link");
        doThrow(cannotExecute).when(persistenceFacade).unLink(student, course);
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(student));
        when(persistenceFacade.findCourseById(id)).thenReturn(Optional.of(course));

        CommandResult<Boolean> result = command.execute(new Long[]{id, id});

        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade).unLink(student, course);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(true)).isFalse();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }

    @Test
    @Disabled
    void shouldNotExecuteCommand_NoStudent() {
        Long id = 132L;

        CommandResult<Boolean> result = command.execute(new Long[]{id, id});

        verify(persistenceFacade).findStudentById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(true)).isFalse();
        assertThat(result.getException()).isInstanceOf(StudentNotExistsException.class);
    }

    @Test
    @Disabled
    void shouldNotExecuteCommand_NoCourse() {
        Long id = 133L;
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(student));

        CommandResult<Boolean> result = command.execute(new Long[]{id, id});

        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade).findCourseById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(true)).isFalse();
        assertThat(result.getException()).isInstanceOf(CourseNotExistsException.class);
    }

    @Test
    void shouldDoCommand() {
        Long id = 130L;
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(student));
        when(persistenceFacade.toEntity(student)).thenReturn(student);
        when(persistenceFacade.findCourseById(id)).thenReturn(Optional.of(course));
        when(persistenceFacade.toEntity(course)).thenReturn(course);
        when(persistenceFacade.unLink(student, course)).thenReturn(true);
        Context<Boolean> context = command.createContext(new Long[]{id, id});

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Boolean result = (Boolean) context.getResult().orElseThrow();
        assertThat(result).isTrue();
        verify(command).executeDo(context);
        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade).toEntity(student);
        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade).toEntity(course);
        assertThat(context.getUndoParameter()).isEqualTo(new Object[]{student, course});

        verify(persistenceFacade).unLink(student, course);
    }

    @Test
    void shouldNotDoCommand_NoStudent() {
        Long id = 132L;
        Context<Boolean> context = command.createContext(new Long[]{id, id});

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentNotExistsException.class);
        verify(command).executeDo(context);
        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade, never()).findCourseById(id);
        verify(persistenceFacade, never()).unLink(student, course);
    }

    @Test
    void shouldNotDoCommand_NoCourse() {
        Long id = 133L;
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(student));
        Context<Boolean> context = command.createContext(new Long[]{id, id});

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CourseNotExistsException.class);
        verify(command).executeDo(context);
        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade, never()).unLink(student, course);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 131L;
        RuntimeException cannotExecute = new RuntimeException("Cannot un-link");
        doThrow(cannotExecute).when(persistenceFacade).unLink(student, course);
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(student));
        when(persistenceFacade.findCourseById(id)).thenReturn(Optional.of(course));
        Context<Boolean> context = command.createContext(new Long[]{id, id});

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistenceFacade).findStudentById(id);
        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade).unLink(student, course);
        assertThat(context.getUndoParameter()).isNull();
    }

    @Test
    void shouldUndoCommand_LinkedParameter() {
        final Object[] forUndo = new Object[]{student, course};
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(forUndo);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(persistenceFacade).link(student, course);
    }

    @Test
    void shouldUndoCommand_IgnoreParameter() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(null);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(persistenceFacade, never()).link(student, course);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter("null");

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(persistenceFacade, never()).link(student, course);
    }

    @Test
    void shouldNotUndoCommand_ExceptionThrown() {
        RuntimeException cannotExecute = new RuntimeException("Cannot un-link");
        doThrow(cannotExecute).when(persistenceFacade).link(student, course);
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(new Object[]{student, course});

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(persistenceFacade).link(student, course);
    }
}