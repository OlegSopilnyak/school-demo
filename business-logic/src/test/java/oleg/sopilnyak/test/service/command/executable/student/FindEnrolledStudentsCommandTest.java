package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.school.common.persistence.education.RegisterPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
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
class FindEnrolledStudentsCommandTest {
    @Mock
    RegisterPersistenceFacade persistence;
    @Mock
    Student instance;
    @Spy
    @InjectMocks
    FindEnrolledStudentsCommand command;

    @Test
    void shouldDoCommand_StudentsNotFound() {
        Long id = 210L;
        Context<Set<Student>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Set<Student> result = context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistence).findEnrolledStudentsByCourseId(id);
    }

    @Test
    void shouldDoCommand_StudentsFound() {
        Long id = 211L;
        when(persistence.findEnrolledStudentsByCourseId(id)).thenReturn(Set.of(instance));
        Context<Set<Student>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Set<Student> result = context.getResult().orElseThrow();
        assertThat(result).isEqualTo(Set.of(instance));
        verify(command).executeDo(context);
        verify(persistence).findEnrolledStudentsByCourseId(id);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 212L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistence).findEnrolledStudentsByCourseId(id);
        Context<Set<Student>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistence).findEnrolledStudentsByCourseId(id);
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