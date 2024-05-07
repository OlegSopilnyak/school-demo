package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.persistence.students.courses.RegisterPersistenceFacade;
import oleg.sopilnyak.test.service.command.type.base.Context;
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
class FindCoursesWithoutStudentsCommandTest {
    @Mock
    RegisterPersistenceFacade persistence;
    @Spy
    @InjectMocks
    FindCoursesWithoutStudentsCommand command;
    @Mock
    Course course;

    @Test
    void shouldDoCommand_CoursesFound() {
        when(persistence.findCoursesWithoutStudents()).thenReturn(Set.of(course));
        Context<Set<Course>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Set<Course> result = context.getResult().orElseThrow();
        assertThat(result).contains(course);
        verify(command).executeDo(context);
        verify(persistence).findCoursesWithoutStudents();
    }

    @Test
    void shouldDoCommand_CoursesNotFound() {
        Context<Set<Course>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Set<Course> result = (Set<Course>) context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistence).findCoursesWithoutStudents();
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistence).findCoursesWithoutStudents();
        Context<Set<Course>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findCoursesWithoutStudents();
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