package oleg.sopilnyak.test.service.command.course;

import oleg.sopilnyak.test.school.common.facade.peristence.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.service.command.CommandResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteCourseCommandTest {
    @Mock
    CoursesPersistenceFacade persistenceFacade;
    @Mock
    Course course;
    @Spy
    @InjectMocks
    DeleteCourseCommand command;

    @Test
    void shouldExecuteCommand() {
        Long id = 100L;

        when(persistenceFacade.findCourseById(id)).thenReturn(Optional.of(course));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade).deleteCourse(id);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult().get()).isFalse();
        assertThat(result.getException()).isNull();

    }
    @Test
    void shouldNotExecuteCommand() {
        Long id = 100L;
        RuntimeException cannotExecute = new RuntimeException("Cannot delete");
        doThrow(cannotExecute).when(persistenceFacade).deleteCourse(id);
        when(persistenceFacade.findCourseById(id)).thenReturn(Optional.of(course));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade).deleteCourse(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isEmpty();
        assertThat(result.getException()).isEqualTo(cannotExecute);

    }
}