package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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
}