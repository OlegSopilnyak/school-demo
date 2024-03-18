package oleg.sopilnyak.test.service.command.executable.course;

import oleg.sopilnyak.test.school.common.exception.CourseNotExistsException;
import oleg.sopilnyak.test.school.common.exception.CourseWithStudentsException;
import oleg.sopilnyak.test.school.common.facade.peristence.students.courses.CoursesPersistenceFacade;
import oleg.sopilnyak.test.school.common.model.Course;
import oleg.sopilnyak.test.school.common.model.Student;
import oleg.sopilnyak.test.service.command.executable.CommandResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
        assertThat(result.getResult().orElse(true)).isFalse();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldNotExecuteCommand_ExceptionThrown() {
        Long id = 101L;
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

    @Test
    void shouldNotExecuteCommand_NoCourse() {
        Long id = 102L;

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade, never()).deleteCourse(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isEmpty();
        assertThat(result.getException()).isInstanceOf(CourseNotExistsException.class);
    }

    @Test
    void shouldNotExecuteCommand_CourseNotEmpty() {
        Long id = 103L;
        Student student = mock(Student.class);
        when(course.getStudents()).thenReturn(List.of(student));
        when(persistenceFacade.findCourseById(id)).thenReturn(Optional.of(course));

        CommandResult<Boolean> result = command.execute(id);

        verify(persistenceFacade).findCourseById(id);
        verify(persistenceFacade, never()).deleteCourse(id);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getResult()).isEmpty();
        assertThat(result.getException()).isInstanceOf(CourseWithStudentsException.class);
    }
}