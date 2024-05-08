package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.exception.NoRoomInTheCourseException;
import oleg.sopilnyak.test.school.common.exception.NotExistCourseException;
import oleg.sopilnyak.test.school.common.exception.NotExistStudentException;
import oleg.sopilnyak.test.school.common.exception.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.StudentCourseLinkPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterStudentToCourseCommandTest {
    @Mock
    StudentCourseLinkPersistenceFacade persistence;
    @Mock
    Course course;
    @Mock
    Student student;
    RegisterStudentToCourseCommand command;

    @BeforeEach
    void setUp() {
        command = spy(new RegisterStudentToCourseCommand(persistence, 2, 2));
    }

    @Test
    void shouldDoCommand_LinkStudentWithCourse() {
        Long id = 120L;
        when(persistence.findStudentById(id)).thenReturn(Optional.of(student));
        when(persistence.toEntity(student)).thenReturn(student);
        when(persistence.findCourseById(id)).thenReturn(Optional.of(course));
        when(persistence.toEntity(course)).thenReturn(course);
        when(persistence.link(student, course)).thenReturn(true);

        Context<Boolean> context = command.createContext(new Long[]{id, id});

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isEqualTo(new StudentToCourseLink(student, course));
        assertThat(context.getResult()).isPresent();
        Boolean result = context.getResult().orElseThrow();
        assertThat(result).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence).findCourseById(id);
        verify(persistence).link(student, course);
    }

    @Test
    void shouldDoCommand_AlreadyLinked() {
        Long id = 125L;
        when(student.getId()).thenReturn(id);
        when(student.getCourses()).thenReturn(List.of(course));
        when(course.getId()).thenReturn(id);
        when(course.getStudents()).thenReturn(List.of(student));
        when(persistence.findStudentById(id)).thenReturn(Optional.of(student));
        when(persistence.findCourseById(id)).thenReturn(Optional.of(course));

        Context<Boolean> context = command.createContext(new Long[]{id, id});

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isNull();
        assertThat(context.getResult()).isPresent();
        Boolean result = context.getResult().orElseThrow();
        assertThat(result).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence).findCourseById(id);
        verify(persistence, never()).link(student, course);
    }

    @Test
    void shouldNotDoCommand_NoStudent() {
        Long id = 121L;
        Context<Boolean> context = command.createContext(new Long[]{id, id});

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistStudentException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence, never()).findCourseById(id);
    }

    @Test
    void shouldNotDoCommand_NoCourse() {
        Long id = 122L;
        when(persistence.findStudentById(id)).thenReturn(Optional.of(student));
        Context<Boolean> context = command.createContext(new Long[]{id, id});

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NotExistCourseException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence).findCourseById(id);
        verify(persistence, never()).link(student, course);
    }

    @Test
    void shouldNotDoCommand_MaximumRooms() {
        Long id = 126L;
        when(course.getStudents()).thenReturn(List.of(student, student));
        when(persistence.findStudentById(id)).thenReturn(Optional.of(student));
        when(persistence.findCourseById(id)).thenReturn(Optional.of(course));
        Context<Boolean> context = command.createContext(new Long[]{id, id});

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NoRoomInTheCourseException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence).findCourseById(id);
        verify(persistence, never()).link(student, course);
    }

    @Test
    void shouldNotDoCommand_CoursesExceed() {
        Long id = 127L;
        when(student.getCourses()).thenReturn(List.of(course, course));
        when(course.getStudents()).thenReturn(List.of(student));
        when(persistence.findStudentById(id)).thenReturn(Optional.of(student));
        when(persistence.findCourseById(id)).thenReturn(Optional.of(course));
        Context<Boolean> context = command.createContext(new Long[]{id, id});

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentCoursesExceedException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence).findCourseById(id);
        verify(persistence, never()).link(student, course);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 123L;
        RuntimeException cannotExecute = new RuntimeException("Cannot link");
        doThrow(cannotExecute).when(persistence).link(student, course);
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
        verify(persistence).link(student, course);
    }

    @Test
    void shouldUndoCommand_Linked() {
        final var forUndo = new StudentToCourseLink(student, course);
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(forUndo);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(persistence).unLink(student, course);
    }

    @Test
    void shouldUndoCommand_NotLinked() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(null);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(persistence, never()).unLink(student, course);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter("null");

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(persistence, never()).unLink(student, course);
    }

    @Test
    void shouldNotUndoCommand_ExceptionThrown() {
        RuntimeException cannotExecute = new RuntimeException("Cannot link");
        doThrow(cannotExecute).when(persistence).unLink(student, course);
        final var forUndo = new StudentToCourseLink(student, course);
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        context.setUndoParameter(forUndo);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(persistence).unLink(student, course);
    }
}