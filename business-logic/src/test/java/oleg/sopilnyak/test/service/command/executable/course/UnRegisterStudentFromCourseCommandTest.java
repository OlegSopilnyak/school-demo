package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.exception.NotExistCourseException;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.StudentCourseLinkPersistenceFacade;
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
class UnRegisterStudentFromCourseCommandTest {
    @Mock
    StudentCourseLinkPersistenceFacade persistence;
    @Mock
    Course course;
    @Mock
    Student student;
    @Spy
    @InjectMocks
    UnRegisterStudentFromCourseCommand command;

    @Test
    void shouldDoCommand_Linked() {
        Long id = 130L;
        when(persistence.findStudentById(id)).thenReturn(Optional.of(student));
        when(persistence.toEntity(student)).thenReturn(student);
        when(persistence.findCourseById(id)).thenReturn(Optional.of(course));
        when(persistence.toEntity(course)).thenReturn(course);
        when(persistence.unLink(student, course)).thenReturn(true);
        Context<Boolean> context = command.createContext(new Long[]{id, id});

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Boolean result = context.getResult().orElseThrow();
        assertThat(result).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence).toEntity(student);
        verify(persistence).findCourseById(id);
        verify(persistence).toEntity(course);
        assertThat(context.<Object>getUndoParameter()).isEqualTo(new StudentToCourseLink(student, course));

        verify(persistence).unLink(student, course);
    }

    @Test
    void shouldNotDoCommand_NoStudent() {
        Long id = 132L;
        Context<Boolean> context = command.createContext(new Long[]{id, id});

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistStudentException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence, never()).findCourseById(id);
        verify(persistence, never()).unLink(student, course);
    }

    @Test
    void shouldNotDoCommand_NoCourse() {
        Long id = 133L;
        when(persistence.findStudentById(id)).thenReturn(Optional.of(student));
        Context<Boolean> context = command.createContext(new Long[]{id, id});

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistCourseException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence).findCourseById(id);
        verify(persistence, never()).unLink(student, course);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 131L;
        RuntimeException cannotExecute = new RuntimeException("Cannot un-link");
        doThrow(cannotExecute).when(persistence).unLink(student, course);
        when(persistence.findStudentById(id)).thenReturn(Optional.of(student));
        when(persistence.findCourseById(id)).thenReturn(Optional.of(course));
        Context<Boolean> context = command.createContext(new Long[]{id, id});

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence).findCourseById(id);
        verify(persistence).unLink(student, course);
        assertThat(context.<Object>getUndoParameter()).isNull();
    }

    @Test
    void shouldUndoCommand_LinkedParameter() {
        final var forUndo = new StudentToCourseLink(student, course);
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(forUndo);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(persistence).link(student, course);
    }

    @Test
    void shouldUndoCommand_IgnoreParameter() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(null);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(persistence, never()).link(student, course);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter("null");

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(persistence, never()).link(student, course);
    }

    @Test
    void shouldNotUndoCommand_ExceptionThrown() {
        final var forUndo = new StudentToCourseLink(student, course);
        RuntimeException cannotExecute = new RuntimeException("Cannot un-link");
        doThrow(cannotExecute).when(persistence).link(student, course);
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(forUndo);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(persistence).link(student, course);
    }
}