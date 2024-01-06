package oleg.sopilnyak.test.service.command.student;

import oleg.sopilnyak.test.school.common.facade.peristence.RegisterPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
import oleg.sopilnyak.test.service.command.executable.student.FindEnrolledStudentsCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

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
    void shouldExecuteCommand() {
        Long id = 210L;

        CommandResult<Set<Student>> result = command.execute(id);

        verify(persistenceFacade).findEnrolledStudentsByCourseId(id);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldExecuteCommand_StudentsFound() {
        Long id = 211L;
        when(persistenceFacade.findEnrolledStudentsByCourseId(id)).thenReturn(Set.of(instance));

        CommandResult<Set<Student>> result = command.execute(id);

        verify(persistenceFacade).findEnrolledStudentsByCourseId(id);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().get().iterator().next()).isEqualTo(instance);
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        Long id = 212L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findEnrolledStudentsByCourseId(id);

        CommandResult<Set<Student>> result = command.execute(id);

        verify(persistenceFacade).findEnrolledStudentsByCourseId(id);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}