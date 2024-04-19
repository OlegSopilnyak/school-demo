package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.StudentsPersistenceFacade;
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

import java.util.Optional;

import static oleg.sopilnyak.test.service.command.type.base.Context.State.DONE;
import static oleg.sopilnyak.test.service.command.type.base.Context.State.UNDONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindStudentCommandTest {
    @Mock
    StudentsPersistenceFacade persistenceFacade;
    @Mock
    Student instance;
    @Spy
    @InjectMocks
    FindStudentCommand command;


    @Test
    @Disabled
    void shouldExecuteCommand() {
        Long id = 106L;

        CommandResult<Optional<Student>> result = command.execute(id);

        verify(persistenceFacade).findStudentById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    @Disabled
    void shouldExecuteCommand_StudentFound() {
        Long id = 107L;
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(instance));

        CommandResult<Optional<Student>> result = command.execute(id);

        verify(persistenceFacade).findStudentById(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).contains(instance);
        assertThat(result.getException()).isNull();
    }

    @Test
    @Disabled
    void shouldNotExecuteCommand() {
        Long id = 108L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findStudentById(id);

        CommandResult<Optional<Student>> result = command.execute(id);

        verify(persistenceFacade).findStudentById(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }

    @Test
    void shouldDoCommand_StudentNotFound() {
        Long id = 106L;
        Context<Optional<Student>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Optional<Student> result = (Optional<Student>) context.getResult().orElseThrow();
        assertThat(result).isEmpty();
        verify(command).executeDo(context);
        verify(persistenceFacade).findStudentById(id);
    }

    @Test
    void shouldDoCommand_StudentFound() {
        Long id = 107L;
        when(persistenceFacade.findStudentById(id)).thenReturn(Optional.of(instance));
        Context<Optional<Student>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isTrue();
        assertThat(context.getResult()).isPresent();
        Optional<Student> result = (Optional<Student>) context.getResult().orElseThrow();
        assertThat(result).contains(instance);
        verify(command).executeDo(context);
        verify(persistenceFacade).findStudentById(id);
    }

    @Test
    void shouldNotDoCommand_ExceptionThrown() {
        Long id = 108L;
        RuntimeException cannotExecute = new RuntimeException();
        doThrow(cannotExecute).when(persistenceFacade).findStudentById(id);
        Context<Optional<Student>> context = command.createContext(id);

        command.doCommand(context);

        assertThat(context.isDone()).isFalse();
        assertThat(context.isFailed()).isTrue();
        assertThat(context.getException()).isEqualTo(cannotExecute);
        verify(command).executeDo(context);
        verify(persistenceFacade).findStudentById(id);
    }

    @Test
    void shouldExecuteCommandUndoCommand() {
        Context<Optional<Student>> context = command.createContext();
        context.setState(DONE);

        command.undoCommand(context);

        assertThat(context.getState()).isEqualTo(UNDONE);
        assertThat(context.getException()).isNull();

        verify(command).executeUndo(context);
    }
}