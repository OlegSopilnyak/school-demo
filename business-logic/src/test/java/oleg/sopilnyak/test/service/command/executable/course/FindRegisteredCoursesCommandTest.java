package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import oleg.sopilnyak.test.service.command.type.base.Context;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindRegisteredCoursesCommandTest {
    @Mock
    PersistenceFacade persistenceFacade;
    @Mock
    Course course;
    @Spy
    @InjectMocks
    FindRegisteredCoursesCommand command;

    @Test
    @Disabled
    void shouldExecuteCommand() {
        Long id = 110L;

        CommandResult<Set<Course>> result = command.execute(id);

        verify(persistenceFacade).findCoursesRegisteredForStudent(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    @Disabled
    void shouldExecuteCommand_FoundCourse() {
        Long id = 111L;
        when(persistenceFacade.findCoursesRegisteredForStudent(id)).thenReturn(Set.of(course));

        CommandResult<Set<Course>> result = command.execute(id);

        verify(persistenceFacade).findCoursesRegisteredForStudent(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get().iterator().next()).isEqualTo(course);
        assertThat(result.getException()).isNull();
    }

    @Test
    @Disabled
    void shouldNotExecuteCommand() {
        Long id = 112L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findCoursesRegisteredForStudent(id);

        CommandResult<Set<Course>> result = command.execute(id);

        verify(persistenceFacade).findCoursesRegisteredForStudent(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }

    @Test
    void shouldDoCommand_CoursesFound() {
        Long id = 111L;
        when(persistenceFacade.findCoursesRegisteredForStudent(id)).thenReturn(Set.of(course));
        Context<Set<Course>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Set<Course> result = (Set<Course>) context.getResult().orElseThrow();
        assertThat(result).contains(course);
        verify(command).executeDo(context);
        verify(persistenceFacade).findCoursesRegisteredForStudent(id);
    }

    @Test
    void shouldDoCommand_CoursesNotFound() {
        Long id = 110L;
        Context<Set<Course>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Set<Course> result = (Set<Course>) context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistenceFacade).findCoursesRegisteredForStudent(id);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 112L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findCoursesRegisteredForStudent(id);
        Context<Set<Course>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistenceFacade).findCoursesRegisteredForStudent(id);
    }

    @Test
    void shouldExecuteCommandUndoCommand() {
        Context<Set<Course>> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();

        verify(command).executeUndo(context);
    }
}