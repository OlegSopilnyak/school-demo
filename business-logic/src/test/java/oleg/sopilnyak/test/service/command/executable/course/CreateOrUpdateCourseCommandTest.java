package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.facade.peristence.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
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
class CreateOrUpdateCourseCommandTest {
    @Mock
    CoursesPersistenceFacade persistenceFacade;
    @Mock
    Course course;
    @Spy
    @InjectMocks
    CreateOrUpdateCourseCommand command;

    @Test
    void shouldExecuteCommand() {
        CommandResult<Optional<Course>> result = command.execute(course);

        verify(persistenceFacade).save(course);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().orElse(Optional.of(mock(Course.class)))).isEmpty();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand() {
        RuntimeException cannotExecute = new RuntimeException("Cannot save");
        doThrow(cannotExecute).when(persistenceFacade).save(course);

        CommandResult<Optional<Course>> result = command.execute(course);

        verify(persistenceFacade).save(course);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult().orElse(Optional.of(mock(Course.class)))).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);
    }
}