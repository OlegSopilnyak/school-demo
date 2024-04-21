package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.school.common.persistence.students.courses.RegisterPersistenceFacade;
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
class FindNotEnrolledStudentsCommandTest {
    @Mock
    RegisterPersistenceFacade persistenceFacade;
    @Mock
    Student instance;
    @Spy
    @InjectMocks
    FindNotEnrolledStudentsCommand command;

    @Test
    @Disabled
    void shouldExecuteCommand() {

        CommandResult<Set<Student>> result = command.execute(null);

        verify(persistenceFacade).findNotEnrolledStudents();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Set.of(mock(Student.class)))).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    @Disabled
    void shouldExecuteCommand_StudentsFound() {
        when(persistenceFacade.findNotEnrolledStudents()).thenReturn(Set.of(instance));

        CommandResult<Set<Student>> result = command.execute(null);

        verify(persistenceFacade).findNotEnrolledStudents();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Set.of(mock(Student.class))).iterator().next()).isEqualTo(instance);
        assertThat(result.getException()).isNull();
    }

    @Test
    @Disabled
    void shouldNotExecuteCommand() {
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findNotEnrolledStudents();

        CommandResult<Set<Student>> result = command.execute(null);

        verify(persistenceFacade).findNotEnrolledStudents();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(Set.of(mock(Student.class)))).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }

    @Test
    void shouldDoCommand_StudentsNotFound() {
        Context<Set<Student>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Set<Student> result = (Set<Student>) context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistenceFacade).findNotEnrolledStudents();
    }

    @Test
    void shouldDoCommand_StudentsFound() {
        when(persistenceFacade.findNotEnrolledStudents()).thenReturn(Set.of(instance));
        Context<Set<Student>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Set<Student> result = (Set<Student>) context.getResult().orElseThrow();
        assertThat(result).isEqualTo(Set.of(instance));
        verify(command).executeDo(context);
        verify(persistenceFacade).findNotEnrolledStudents();
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findNotEnrolledStudents();
        Context<Set<Student>> context = command.createContext(null);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistenceFacade).findNotEnrolledStudents();
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