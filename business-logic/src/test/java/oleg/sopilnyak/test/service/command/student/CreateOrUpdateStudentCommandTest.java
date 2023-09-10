package oleg.sopilnyak.test.service.command.student;

import oleg.sopilnyak.test.school.common.facade.peristence.StudentsPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.CommandResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CreateOrUpdateStudentCommandTest {
    @Mock
    StudentsPersistenceFacade persistenceFacade;
    @Mock
    Student student;
    @Spy
    @InjectMocks
    CreateOrUpdateStudentCommand command;

    @Test
    void shouldExecuteCommand() {

        CommandResult<Optional<Student>> result = command.execute(student);

        verify(persistenceFacade).save(student);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isNull();
    }
    @Test
    void shouldNotExecuteCommand() {
        RuntimeException cannotExecute = new RuntimeException("Cannot save");
        doThrow(cannotExecute).when(persistenceFacade).save(student);

        CommandResult<Optional<Student>> result = command.execute(student);

        verify(persistenceFacade).save(student);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}