package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.exception.education.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.exception.education.CourseNotFoundException;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.school.common.persistence.education.CoursesPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.sys.CommandContext;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.school.common.exception.core.InvalidParameterTypeException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteCourseCommandTest {
    @Mock
    Course mockedCourse;
    @Mock
    CoursePayload mockedCoursePayload;
    @Mock
    CoursesPersistenceFacade persistence;
    @Mock
    BusinessMessagePayloadMapper payloadMapper;
    @Spy
    @InjectMocks
    DeleteCourseCommand command;

    @Test
    void shouldBeValidCommand() {
        assertThat(command).isNotNull();
        assertThat(persistence).isEqualTo(ReflectionTestUtils.getField(command, "persistenceFacade"));
        assertThat(payloadMapper).isEqualTo(ReflectionTestUtils.getField(command, "payloadMapper"));
    }

    @Test
    void shouldDoCommand_CourseFound() {
        Long id = 100L;
        when(persistence.findCourseById(id)).thenReturn(Optional.of(mockedCourse));
        when(payloadMapper.toPayload(mockedCourse)).thenReturn(mockedCoursePayload);
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isEqualTo(mockedCoursePayload);
        assertThat(context.getResult()).isPresent();
        Boolean result = context.getResult().orElseThrow();
        assertThat(result).isTrue();
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
        verify(payloadMapper).toPayload(mockedCourse);
        verify(persistence).deleteCourse(id);
    }

    @Test
    void shouldNotDoCommand_CourseNotFound() {
        Long id = 102L;
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isNull();
        assertThat(context.getException()).isInstanceOf(CourseNotFoundException.class);
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
        verify(persistence, never()).deleteCourse(id);
    }

    @Test
    void shouldNotDoCommand_CourseHasEnrolledStudent() {
        Long id = 113L;
        when(mockedCoursePayload.getStudents()).thenReturn(List.of(mock(Student.class)));
        when(persistence.findCourseById(id)).thenReturn(Optional.of(mockedCourse));
        when(payloadMapper.toPayload(mockedCourse)).thenReturn(mockedCoursePayload);
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(CourseWithStudentsException.class);
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
        verify(payloadMapper).toPayload(mockedCourse);
        verify(persistence, never()).deleteCourse(id);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 101L;
        RuntimeException cannotExecute = new RuntimeException("Cannot delete");
        doThrow(cannotExecute).when(persistence).deleteCourse(id);
        when(persistence.findCourseById(id)).thenReturn(Optional.of(mockedCourse));
        when(payloadMapper.toPayload(mockedCourse)).thenReturn(mockedCoursePayload);
        Context<Boolean> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
        verify(payloadMapper).toPayload(mockedCourse);
        verify(persistence).deleteCourse(id);
    }

    @Test
    void shouldUndoCommand_CourseFound() {
        Context<Boolean> context = command.createContext();
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setState(Context.State.DONE);
            commandContext.setUndoParameter(Input.of(mockedCourse));
        }
//        context.setState(DONE);
//        context.setUndoParameter(mockedCourse);
        when(persistence.save(mockedCourse)).thenReturn(Optional.of(mockedCourse));

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();
        verify(command).executeUndo(context);
        verify(persistence).save(mockedCourse);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Boolean> context = command.createContext();
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setState(Context.State.DONE);
            commandContext.setUndoParameter(Input.of("course"));
        }
//        context.setState(DONE);
//        context.setUndoParameter("course");

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Parameter not a 'Course' value:[course]");
        verify(command).executeUndo(context);
        verify(persistence, never()).save(mockedCourse);
    }

    @Test
    void shouldNotUndoCommand_NullParameter() {
        Context<Boolean> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        verify(command).executeUndo(context);
        verify(persistence, never()).save(mockedCourse);
    }

    @Test
    void shouldNotUndoCommand_ExceptionThrown() {
        Context<Boolean> context = command.createContext();
        RuntimeException cannotExecute = new RuntimeException("Cannot restore");
        doThrow(cannotExecute).when(persistence).save(mockedCourse);
        if (context instanceof CommandContext<?> commandContext) {
            commandContext.setState(Context.State.DONE);
            commandContext.setUndoParameter(Input.of(mockedCourse));
        }
//        context.setState(DONE);
//        context.setUndoParameter(mockedCourse);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);

        verify(command).executeUndo(context);
        verify(persistence).save(mockedCourse);
    }
}