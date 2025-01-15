package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.exception.education.CourseHasNoRoomException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentNotFoundException;
import oleg.sopilnyak.test.school.common.exception.education.StudentCoursesExceedException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.joint.EducationPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;
import oleg.sopilnyak.test.service.message.payload.StudentPayload;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterStudentToCourseCommandTest {
    @Mock
    Course course;
    @Mock
    CoursePayload coursePayload;
    @Mock
    Student student;
    @Mock
    StudentPayload studentPayload;
    @Mock
    EducationPersistenceFacade persistence;
    @Mock
    BusinessMessagePayloadMapper payloadMapper;
    RegisterStudentToCourseCommand command;

    @BeforeEach
    void setUp() {
        command = spy(new RegisterStudentToCourseCommand(persistence, payloadMapper, 2, 2));
    }

    @Test
    void shouldBeValidCommand() {
        assertThat(command).isNotNull();
        assertThat(persistence).isEqualTo(ReflectionTestUtils.getField(command, "persistenceFacade"));
        assertThat(payloadMapper).isEqualTo(ReflectionTestUtils.getField(command, "payloadMapper"));
    }

    @Test
    void shouldDoCommand_LinkStudentWithCourse() {
        Long id = 120L;
        Long courseId = 1201L;
        Long studentId = 1202L;
        when(student.getId()).thenReturn(studentId);
        when(course.getId()).thenReturn(courseId);
        when(persistence.findStudentById(id)).thenReturn(Optional.of(student));
        when(persistence.findCourseById(id)).thenReturn(Optional.of(course));
        when(persistence.link(student, course)).thenReturn(true);

        Context<Boolean> context = command.createContext(Input.of(id, id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getUndoParameter().value()).isEqualTo(Input.of(studentId, courseId));
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
        when(course.getId()).thenReturn(id);
        when(student.getCourses()).thenReturn(List.of(course));
        when(course.getStudents()).thenReturn(List.of(student));
        when(persistence.findStudentById(id)).thenReturn(Optional.of(student));
        when(persistence.findCourseById(id)).thenReturn(Optional.of(course));

        Context<Boolean> context = command.createContext(Input.of(id, id));

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
        Context<Boolean> context = command.createContext(Input.of(id, id));

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(StudentNotFoundException.class);
        verify(command).executeDo(context);
        verify(persistence).findStudentById(id);
        verify(persistence, never()).findCourseById(id);
    }

    @Test
    void shouldNotDoCommand_NoCourse() {
        Long id = 122L;
        when(persistence.findStudentById(id)).thenReturn(Optional.of(student));
        Context<Boolean> context = command.createContext(Input.of(id, id));

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CourseNotFoundException.class);
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
        Context<Boolean> context = command.createContext(Input.of(id, id));

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CourseHasNoRoomException.class);
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
        Context<Boolean> context = command.createContext(Input.of(id, id));

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
        Context<Boolean> context = command.createContext(Input.of(id, id));

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
        Long id = 124L;
        when(persistence.findStudentById(id)).thenReturn(Optional.of(student));
        when(persistence.findCourseById(id)).thenReturn(Optional.of(course));
        Context<Boolean> context = command.createContext();

        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id, id));
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(persistence).unLink(student, course);
    }

    @Test
    void shouldUndoCommand_NotLinked_InputIsNull() {
        Context<Boolean> context = command.createContext();

        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(persistence, never()).unLink(any(Student.class), any(Course.class));
    }

    @Test
    void shouldUndoCommand_NotLinked_InputIsEmpty() {
        Context<Boolean> context = command.createContext();

        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.empty());
        }

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        verify(persistence, never()).unLink(any(Student.class), any(Course.class));
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Boolean> context = command.createContext();
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setState(Context.State.DONE);
            commandContext.setUndoParameter(Input.of("null"));
        }
//        context.setState(Context.State.DONE);
//        context.setUndoParameter("null");

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(ClassCastException.class);
        verify(persistence, never()).unLink(student, course);
    }

    @Test
    void shouldNotUndoCommand_ExceptionThrown() {
        Long id = 128L;
        when(persistence.findStudentById(id)).thenReturn(Optional.of(student));
        when(persistence.findCourseById(id)).thenReturn(Optional.of(course));
        RuntimeException cannotExecute = new RuntimeException("Cannot link");
        Context<Boolean> context = command.createContext();
        context.setState(Context.State.DONE);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setUndoParameter(Input.of(id, id));
        }

        doThrow(cannotExecute).when(persistence).unLink(student, course);
        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(persistence).unLink(student, course);
    }
}