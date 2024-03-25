package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
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
class FindCoursesWithoutStudentsCommandTest {
    @Mock
    PersistenceFacade persistenceFacade;
    @Spy
    @InjectMocks
    FindCoursesWithoutStudentsCommand command;

    @Test
    void shouldExecuteCommand() {

        CommandResult<Set<Course>> result = command.execute(null);

        verify(persistenceFacade).findCoursesWithoutStudents();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Set.of(mock(Course.class)))).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findCoursesWithoutStudents();

        CommandResult<Set<Course>> result = command.execute(null);

        verify(persistenceFacade).findCoursesWithoutStudents();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(Set.of(mock(Course.class)))).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}