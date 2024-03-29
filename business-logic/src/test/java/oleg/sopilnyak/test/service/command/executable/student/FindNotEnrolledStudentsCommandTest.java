package oleg.sopilnyak.test.service.command.executable.student;

import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.RegisterPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.sys.CommandResult;
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
class FindNotEnrolledStudentsCommandTest {
    @Mock
    RegisterPersistenceFacade persistenceFacade;
    @Mock
    Student instance;
    @Spy
    @InjectMocks
    FindNotEnrolledStudentsCommand command;

    @Test
    void shouldExecuteCommand() {

        CommandResult<Set<Student>> result = command.execute(null);

        verify(persistenceFacade).findNotEnrolledStudents();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Set.of(mock(Student.class)))).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldExecuteCommand_StudentsFound() {
        when(persistenceFacade.findNotEnrolledStudents()).thenReturn(Set.of(instance));

        CommandResult<Set<Student>> result = command.execute(null);

        verify(persistenceFacade).findNotEnrolledStudents();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Set.of(mock(Student.class))).iterator().next()).isEqualTo(instance);
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findNotEnrolledStudents();

        CommandResult<Set<Student>> result = command.execute(null);

        verify(persistenceFacade).findNotEnrolledStudents();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(Set.of(mock(Student.class)))).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}