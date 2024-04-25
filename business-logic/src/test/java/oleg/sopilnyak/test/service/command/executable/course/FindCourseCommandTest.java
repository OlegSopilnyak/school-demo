package oleg.sopilnyak.test.service.command.executable.course;

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

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindCourseCommandTest {
    @Mock
    CoursesPersistenceFacade persistenceFacade;
    @Mock
    Course course;
    @Spy
    @InjectMocks
    FindCourseCommand command;

    @Test
    @Disabled
    void shouldExecuteCommand() {
        Long id = 102L;

        CommandResult<Optional<Course>> result = command.execute(id);

        verify(persistenceFacade).findCourseById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    @Disabled
    void shouldExecuteCommand_CourseFound() {
        Long id = 103L;
        when(persistenceFacade.findCourseById(id)).thenReturn(Optional.of(course));

        CommandResult<Optional<Course>> result = command.execute(id);

        verify(persistenceFacade).findCourseById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).contains(course);
        assertThat(result.getException()).isNull();
    }

    @Test
    @Disabled
    void shouldNotExecuteCommand() {
        Long id = 104L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findCourseById(id);

        CommandResult<Optional<Course>> result = command.execute(id);

        verify(persistenceFacade).findCourseById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }

    @Test
    void shouldDoCommand_CourseFound() {
        Long id = 103L;
        when(persistenceFacade.findCourseById(id)).thenReturn(Optional.of(course));
        Context<Optional<Course>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Optional<Course> result = (Optional<Course>) context.getResult().orElseThrow();
        assertThat(result).contains(course);
        verify(command).executeDo(context);
        verify(persistenceFacade).findCourseById(id);
    }

    @Test
    void shouldDoCommand_CourseNotFound() {
        Long id = 102L;
        Context<Optional<Course>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Optional<Course> result = (Optional<Course>) context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistenceFacade).findCourseById(id);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 104L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findCourseById(id);
        Context<Optional<Course>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistenceFacade).findCourseById(id);
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