package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.RegisterPersistenceFacade;
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

import java.util.Set;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindEnrolledStudentsCommandTest {
    @Mock
    RegisterPersistenceFacade persistenceFacade;
    @Mock
    Student instance;
    @Spy
    @InjectMocks
    FindEnrolledStudentsCommand command;

    @Test
    @Disabled
    void shouldExecuteCommand() {
        Long id = 210L;

        CommandResult<Set<Student>> result = command.execute(id);

        verify(persistenceFacade).findEnrolledStudentsByCourseId(id);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Set.of(mock(Student.class)))).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    @Disabled
    void shouldExecuteCommand_StudentsFound() {
        Long id = 211L;
        when(persistenceFacade.findEnrolledStudentsByCourseId(id)).thenReturn(Set.of(instance));

        CommandResult<Set<Student>> result = command.execute(id);

        verify(persistenceFacade).findEnrolledStudentsByCourseId(id);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Set.of(mock(Student.class))).iterator().next()).isEqualTo(instance);
        assertThat(result.getException()).isNull();
    }

    @Test
    @Disabled
    void shouldNotExecuteCommand() {
        Long id = 212L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findEnrolledStudentsByCourseId(id);

        CommandResult<Set<Student>> result = command.execute(id);

        verify(persistenceFacade).findEnrolledStudentsByCourseId(id);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(Set.of(mock(Student.class)))).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }

    @Test
    void shouldDoCommand_StudentsNotFound() {
        Long id = 210L;
        Context<Set<Student>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Set<Student> result = (Set<Student>) context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistenceFacade).findEnrolledStudentsByCourseId(id);
    }

    @Test
    void shouldDoCommand_StudentsFound() {
        Long id = 211L;
        when(persistenceFacade.findEnrolledStudentsByCourseId(id)).thenReturn(Set.of(instance));
        Context<Set<Student>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Set<Student> result = (Set<Student>) context.getResult().orElseThrow();
        assertThat(result).isEqualTo(Set.of(instance));
        verify(command).executeDo(context);
        verify(persistenceFacade).findEnrolledStudentsByCourseId(id);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 212L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findEnrolledStudentsByCourseId(id);
        Context<Set<Student>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistenceFacade).findEnrolledStudentsByCourseId(id);
    }

    @Test
    void shouldExecuteCommandUndoCommand() {
        Context<Set<Student>> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();

        verify(command).executeUndo(context);
    }
}