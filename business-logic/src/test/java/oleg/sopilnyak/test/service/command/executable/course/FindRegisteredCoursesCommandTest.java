package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.facade.PersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
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
class FindRegisteredCoursesCommandTest {
    @Mock
    PersistenceFacade persistenceFacade;
    @Mock
    Course course;
    @Spy
    @InjectMocks
    FindRegisteredCoursesCommand command;

    @Test
    void shouldExecuteCommand() {
        Long id = 110L;

        CommandResult<Set<Course>> result = command.execute(id);

        verify(persistenceFacade).findCoursesRegisteredForStudent(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldExecuteCommand_FoundCourse() {
        Long id = 111L;
        when(persistenceFacade.findCoursesRegisteredForStudent(id)).thenReturn(Set.of(course));

        CommandResult<Set<Course>> result = command.execute(id);

        verify(persistenceFacade).findCoursesRegisteredForStudent(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get().iterator().next()).isEqualTo(course);
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        Long id = 112L;
        RuntimeException cannotExecute = new RuntimeException("Cannot find");
        doThrow(cannotExecute).when(persistenceFacade).findCoursesRegisteredForStudent(id);

        CommandResult<Set<Course>> result = command.execute(id);

        verify(persistenceFacade).findCoursesRegisteredForStudent(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isPresent();
        assertThat(result.getResult().get()).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}