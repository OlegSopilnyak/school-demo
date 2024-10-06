package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.students.courses.CoursesPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.exception.InvalidParameterTypeException;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.CoursePayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateOrUpdateCourseCommandTest {
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
    CreateOrUpdateCourseCommand command;

    @Test
    void shouldBeValidCommand() {
        assertThat(command).isNotNull();
        assertThat(persistence).isEqualTo(ReflectionTestUtils.getField(command, "persistence"));
        assertThat(payloadMapper).isEqualTo(ReflectionTestUtils.getField(command, "payloadMapper"));
    }

    @Test
    void shouldDoCommand_CreateCourse() {
        Long id = -100L;
        when(mockedCourse.getId()).thenReturn(id);
        when(persistence.save(mockedCourse)).thenReturn(Optional.of(mockedCourse));

        Context<Optional<Course>> context = command.createContext(mockedCourse);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isEqualTo(id);
        assertThat(context.getResult()).isPresent();
        Optional<Course> result = context.getResult().orElseThrow();
        assertThat(result).contains(mockedCourse);
        verify(command).executeDo(context);
        verify(persistence).save(mockedCourse);
    }

    @Test
    void shouldDoCommand_UpdateCourse() {
        Long id = 100L;
        when(mockedCourse.getId()).thenReturn(id);
        when(persistence.findCourseById(id)).thenReturn(Optional.of(mockedCourse));
        when(payloadMapper.toPayload(mockedCourse)).thenReturn(mockedCoursePayload);
        when(persistence.save(mockedCourse)).thenReturn(Optional.of(mockedCourse));
        Context<Optional<Course>> context = command.createContext(mockedCourse);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.<Object>getUndoParameter()).isEqualTo(mockedCoursePayload);
        assertThat(context.getResult()).isPresent();
        Optional<Course> result = context.getResult().orElseThrow();
        assertThat(result).contains(mockedCourse);
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
        verify(payloadMapper).toPayload(mockedCourse);
        verify(persistence).save(mockedCourse);
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
        when(mockedCourse.getId()).thenReturn(id);
        RuntimeException cannotExecute = new RuntimeException("Cannot create");
        when(persistence.save(mockedCourse)).thenThrow(cannotExecute).thenReturn(Optional.of(mockedCourse));

        Context<Optional<Course>> context = command.createContext(mockedCourse);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).save(mockedCourse);
    }

    @Test
    void shouldNotDoCommand_UpdateExceptionThrown() {
        Long id = 102L;
        when(mockedCourse.getId()).thenReturn(id);
        when(persistence.findCourseById(id)).thenReturn(Optional.of(mockedCourse));
        when(payloadMapper.toPayload(mockedCourse)).thenReturn(mockedCoursePayload);
        RuntimeException cannotExecute = new RuntimeException("Cannot update");
        doThrow(cannotExecute).when(persistence).save(any(Course.class));

        Context<Optional<Course>> context = command.createContext(mockedCourse);

        assertThrows(RuntimeException.class, () -> command.doCommand(context));

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
        verify(persistence).save(mockedCourse);
        verify(persistence).save(mockedCoursePayload);
    }

    @Test
    void shouldUndoCommand_CreateCourse() {
        Long id = 103L;
        Context<Optional<Course>> context = command.createContext(mockedCourse);
        context.setState(Context.State.DONE);
        context.setUndoParameter(id);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).deleteCourse(id);
    }

    @Test
    void shouldUndoCommand_UpdateCourse() {
        Context<Optional<Course>> context = command.createContext(mockedCourse);
        context.setState(Context.State.DONE);
        context.setUndoParameter(mockedCourse);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(Context.State.UNDONE);
        verify(command).executeUndo(context);
        verify(persistence).save(mockedCourse);
    }

    @Test
    void shouldNotUndoCommand_WrongParameterType() {
        Context<Optional<Course>> context = command.createContext(mockedCourse);
        context.setState(Context.State.DONE);
        context.setUndoParameter("id");

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(InvalidParameterTypeException.class);
        assertThat(context.getException().getMessage()).startsWith("Parameter not a  'Long' value:[id]");
        verify(command).executeUndo(context);
        verify(persistence, never()).deleteCourse(anyLong());
    }

    @Test
    void shouldNotUndoCommand_NullParameter() {
        Context<Optional<Course>> context = command.createContext(mockedCourse);
        context.setState(Context.State.DONE);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isInstanceOf(NullPointerException.class);
        assertThat(context.getException().getMessage()).isEqualTo("Wrong input parameter value null");
        verify(command).executeUndo(context);
        verify(persistence, never()).deleteCourse(anyLong());
    }

    @Test
    void shouldNotUndoCommand_CreateExceptionThrown() {
        Long id = 104L;
        Context<Optional<Course>> context = command.createContext(mockedCourse);
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
        Context<Optional<Course>> context = command.createContext(mockedCourse);
        context.setState(Context.State.DONE);
        context.setUndoParameter(mockedCourse);
        RuntimeException cannotExecute = new RuntimeException("Cannot undo update");
        doThrow(cannotExecute).when(persistence).save(mockedCourse);

        command.undoCommand(context);

        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeUndo(context);
        verify(persistence).save(mockedCourse);
    }
}