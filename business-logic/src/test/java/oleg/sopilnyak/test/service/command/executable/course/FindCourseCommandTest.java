package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.education.CoursesPersistenceFacade;
import oleg.sopilnyak.test.service.command.executable.education.course.FindCourseCommand;
import oleg.sopilnyak.test.service.command.io.Input;
import oleg.sopilnyak.test.service.command.type.base.Context;
import oleg.sopilnyak.test.service.mapper.BusinessMessagePayloadMapper;
import oleg.sopilnyak.test.service.message.payload.CoursePayload;
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
class FindCourseCommandTest {
    @Mock
    CoursesPersistenceFacade persistence;
    @Mock
    BusinessMessagePayloadMapper payloadMapper;
    @Mock
    Course course;
    @Mock
    CoursePayload mockedCoursePayload;
    @Spy
    @InjectMocks
    FindCourseCommand command;

    @Test
    void shouldDoCommand_CourseFound() {
        Long id = 103L;
        when(persistence.findCourseById(id)).thenReturn(Optional.of(course));
        when(payloadMapper.toPayload(course)).thenReturn(mockedCoursePayload);
        Context<Optional<Course>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Optional<Course> result = context.getResult().orElseThrow();
        assertThat(result).contains(course);
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
    }

    @Test
    void shouldDoCommand_CourseNotFound() {
        Long id = 102L;
        Context<Optional<Course>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Optional<Course> result = context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 104L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistence).findCourseById(id);
        Context<Optional<Course>> context = command.createContext(Input.of(id));

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findCourseById(id);
    }

    @Test
    void shouldExecuteCommandUndoCommand() {
        Context<Optional<Course>> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();

        verify(command).executeUndo(context);
    }
}